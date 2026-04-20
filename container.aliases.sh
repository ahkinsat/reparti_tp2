# Development (after compile, before package)
alias cli='java -cp target/classes tp2.Cli'
# Production (after package)
alias jcli='java -jar target/tp2-1.0.0.jar'
alias ho-consumer='java -cp target/tp2-1.0.0.jar tp2.ho.EventConsumer'
alias bo1-processor='java -cp target/tp2-1.0.0.jar tp2.bo.OutboxProcessor 1'
alias bo2-processor='java -cp target/tp2-1.0.0.jar tp2.bo.OutboxProcessor 2'


