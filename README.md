## TP2 README - Database Synchronization with RabbitMQ

### Project Structure
```
.
├── docker-compose.yml
├── javadev.Dockerfile
├── pom.xml
├── .env
├── .env.example
├── aliases.sh
├── README.md
└── src/main/java/tp2/
   ├── Cli.java                # CLI router (commands dispatch)
   ├── Config.java             # Environment variables reader
   ├── Database.java           # Plain JDBC connections (DriverManager)
   ├── Init.java               # Idempotent DB/queue setup
   ├── RabbitMqManager.java    # Channel & queue management
   ├── Util.java               # JSON, parsing helpers
   ├── models/
   │   ├── Sale.java           # Sale entity
   │   └── Message.java        # RabbitMQ message with JSON serde
   ├── bo/
   │   ├── BranchOfficeApi.java   # BO: sales CRUD + outbox insert
   │   ├── OutboxProcessor.java   # Sends unsent events (standalone)
   │   └── OutboxRecord.java      # Outbox row record
   └── ho/
       ├── HeadOfficeApi.java     # HO: queries for sales/events/syncs
       ├── EventConsumer.java     # Consumes & applies events (standalone)
       └── SyncRecord.java        # Sync state record
```

### Setup

0. **Load aliases**
   ```bash
   source aliases.sh
   ```

1. **Start containers:**
   ```bash
   docker-compose up -d
   ```

2. **Enter dev container:**
   ```bash
   java-shell
   ```

3. **Build and Initialize:**
   ```bash
   mvn clean package
   jcli init
   ```

### Run Services (3 terminals)

```bash
# Terminal 1 - Head Office Consumer
java -cp target/tp2-1.0.0.jar tp2.ho.EventConsumer

# Terminal 2 - BO1 Outbox Processor
java -cp target/tp2-1.0.0.jar tp2.bo.OutboxProcessor 1

# Terminal 3 - BO2 Outbox Processor
java -cp target/tp2-1.0.0.jar tp2.bo.OutboxProcessor 2
```

### Network Simulation

```bash
# Disconnect services (simulate outage)
net-down-rmq
net-down-ho
net-down-bo1
net-down-bo2

# Reconnect services
net-up-rmq
net-up-ho
net-up-bo1
net-up-bo2
```

### CLI Commands

| Command | Description |
|---------|-------------|
| `jcli init` | Idempotent setup |
| `jcli config` | Show current config |
| `jcli bo 1 sales write 2025-04-15 North Laptop 5 1000 1200 200 1400` | Write sale |
| `jcli bo 1 sales del <sale_id>` | Delete sale |
| `jcli bo 1 sales list --limit 10` | List sales (CSV) |
| `jcli bo 1 sales get <sale_id>` | Get single sale (CSV) |
| `jcli bo 1 events list --limit 10` | List outbox events |
| `jcli bo 1 events get <event_id>` | Get outbox event |
| `jcli ho sales list --limit 10` | List HO consolidated sales |
| `jcli ho sales get <sale_id>` | Get HO sale |
| `jcli ho events list --limit 10` | List processed events |
| `jcli ho events get <event_id>` | Get event |
| `jcli ho syncs list --limit 10` | List sync state |
| `jcli ho syncs get <sale_id>` | Get sync state |

### Database Schema

**Branch Office (BO1/BO2):**
- `sales` – local current state
- `outbox` – pending events (sent=false, retry_count, payload JSONB)

**Head Office:**
- `sales` – consolidated final state
- `events` – idempotency log (applied_at = NULL for old events)
- `syncs` – per‑sale last event (time + id for ordering)

### Message Format (JSON)
```json
{
  "eventId": "uuid",
  "type": "WRITE",
  "eventTime": "2025-04-15T10:30:00.123Z",
  "saleId": "uuid",
  "sale": { /* full sale object, null for DELETE */ }
}
```

### Demo

```bash
# Write sales
jcli bo 1 sales write 2025-04-15 North Laptop 5 1000 1200 200 1400
jcli bo 2 sales write 2025-04-15 East Mouse 10 50 500 100 600

# Check HO
jcli ho sales list

# Simulate outage
net-down-rmq

# Write during outage (queues in outbox)
jcli bo 1 sales write 2025-04-15 South Keyboard 3 75 225 18 243

# Restore connection
net-up-rmq

# Wait 10 seconds, then verify all data synced
jcli ho sales list
```

### Cleanup
```bash
docker-compose down -v
```

### Architecture Notes
- **Outbox pattern** – handles 1-2h/day internet outages
- **Idempotency** – events table prevents duplicate processing
- **Ordering** – (event_time, event_id) tie-breaker
- **Retries** – 3 attempts, then manual inspection
- **No DLQ** – failed events stay in outbox for manual recovery
