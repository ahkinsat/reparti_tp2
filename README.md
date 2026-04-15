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
   ├── ServiceManager.java     # Background thread lifecycle
   ├── Util.java               # CSV, JSON, parsing helpers
   ├── models/
   │   ├── Sale.java           # Sale record (Java 21)
   │   └── Message.java        # RabbitMQ message record with JSON serde
   ├── bo/
   │   ├── BranchOfficeApi.java   # BO: sales CRUD + outbox insert
   │   ├── OutboxProcessor.java   # Background: sends unsent events
   │   └── OutboxRecord.java      # Outbox row record
   └── ho/
       ├── HeadOfficeApi.java     # HO: queries for sales/events/syncs
       ├── EventConsumer.java     # Background: consumes & applies events
       └── SyncRecord.java        # Sync state record
```

### Setup

0. **Load aliases**
   ```bash
   . aliases.sh
   ```

1. **Start containers:**
   ```bash
   docker-compose up -d
   ```

2. **Enter dev container:**
   ```bash
   java-shell    # enters javadev container (auto load aliases)
   ```

3. **Initialize (once):**
   ```bash
   mvn clean compile package 
   jcli init # use jcli (fat jar), cli may not always work(thin misses deps)
   ```
   Creates all tables (HO: sales/events/syncs, BO1/BO2: sales/outbox) and RabbitMQ queue.


### Network Simulation (Podman)

Use these aliases to simulate internet outages (1-2h/day connectivity):

```bash
# Disconnect services
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
| `cli init` | Idempotent setup |
| `cli config` | Show current config |
| `cli bo 1 sales write 2025-04-15 North Laptop 5 1000 1200 200 1400` | Write sale |
| `cli bo 1 sales del <sale_id>` | Delete sale |
| `cli bo 1 sales list --limit 10` | List sales (CSV) |
| `cli bo 1 sales get <sale_id>` | Get single sale (CSV) |
| `cli bo 1 events list --limit 10` | List outbox events |
| `cli bo 1 events get <event_id>` | Get outbox event |
| `cli bo 1 service start` | Start outbox processor |
| `cli bo 1 service stop` | Stop outbox processor |
| `cli bo 1 service status` | Check status |
| `cli ho sales list --limit 10` | List HO consolidated sales |
| `cli ho sales get <sale_id>` | Get HO sale |
| `cli ho events list --limit 10` | List processed events |
| `cli ho events get <event_id>` | Get event |
| `cli ho syncs list --limit 10` | List sync state |
| `cli ho syncs get <sale_id>` | Get sync state |
| `cli ho service start` | Start HO consumer |
| `cli ho service stop` | Stop HO consumer |
| `cli service start` | Start all (BO1+BO2+HO) |
| `cli service stop` | Stop all |
| `cli service status` | Status all |

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
  "type": "WRITE",           // or "DELETE"
  "eventTime": "2025-04-15T10:30:00.123Z",
  "saleId": "uuid",
  "sale": { /* full sale object, null for DELETE */ }
}
```

### Running the System

```bash
# Start all services
cli service start

# Write some sales (BO1)
cli bo 1 sales write 2025-04-15 North Laptop 5 1000 1200 200 1400
cli bo 1 sales write 2025-04-15 South Mouse 10 50 500 100 600

# Check HO (events appear within seconds)
cli ho sales list

# Stop everything
cli service stop
```

### Cleanup
```bash
docker-compose down -v   # removes containers + volumes
```

---

**Note:** Internet assumed 1-2h/day → outbox stores events offline. RabbitMQ queue durable, no DLQ (manual outbox inspection after 3 retries). Ordering via `(event_time, event_id)` tie-break.
