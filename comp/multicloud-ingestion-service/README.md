# Data Ingestion Service
## Responsibility: 
- To ingest billing data from AWS, Azure, and GCP multiple times a day, and forward it to the main application.
- Before forwarding it to the Java based monolith, it will adapt the data to the custom, vendor-agnostic format, similar to the FOCUS format.

## Infrastructure:
- Runs in a separate container.
- Instead of polling BLOB storages directly, it uses each of their respective messaging queues, operating using an event driven architecture.
- Forwards this data to the Java based monolith via AMQP. Payload is serialized in Protocol Buffers.