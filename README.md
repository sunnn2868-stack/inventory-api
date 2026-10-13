# Inventory API

A Spring Boot REST API that integrates with an Order Management API using REST and RabbitMQ. The project demonstrates asynchronous event processing, message retries, dead-letter queues, and PostgreSQL-backed idempotency.

## Technology Stack

* Java 21
* Spring Boot
* Maven
* PostgreSQL
* RabbitMQ
* Docker
* JUnit and Mockito
* Testcontainers

## Key Features

* REST endpoints for inventory operations.
* Integration with an Order Management API.
* Asynchronous consumption of `order.created` events from RabbitMQ.
* Retry configuration for transient message-processing failures.
* Dead-letter queue handling for messages that cannot be processed successfully.
* PostgreSQL-backed idempotency to avoid processing the same order event more than once.
* Unit and integration tests.

## Event Processing Flow

1. The Order Management API validates an order against the Inventory API.
2. After an order is saved successfully, the Order Management API publishes an `OrderCreatedEvent`.
3. RabbitMQ delivers the event through the `order.created` queue.
4. The Inventory API deserializes the message and checks its order ID against PostgreSQL.
5. If the order event has already been processed, the duplicate is ignored.
6. If the event is new, the listener processes it.
7. If processing fails, RabbitMQ listener retry configuration determines how the message is retried.
8. Messages that remain unsuccessful after retry handling can be routed to a dead-letter queue, provided the queue's dead-letter exchange and routing configuration are correctly configured.

## RabbitMQ Configuration

The application uses the following retry settings in its local configuration:

```properties
spring.rabbitmq.listener.simple.retry.enabled=true
spring.rabbitmq.listener.simple.retry.initial-interval=1s
spring.rabbitmq.listener.simple.retry.max-attempts=3
spring.rabbitmq.listener.simple.retry.multiplier=2
spring.rabbitmq.listener.simple.default-requeue-rejected=false
```

These settings enable listener retries with an initial one-second interval and a multiplier of two. The configured maximum attempts value is `3`.

With `default-requeue-rejected=false`, a message rejected after listener failure is not automatically returned to the main queue. If a dead-letter exchange is configured for the queue, the broker can route the rejected message to the associated dead-letter queue.

**Configuration note:** These properties are stored in the local `application-local.properties` file. Because local configuration files are ignored by Git, configure these properties in your own local configuration when setting up the project. Do not commit database passwords or other secrets.

## Idempotency

The Inventory API records processed order event IDs in PostgreSQL using the `processed_order_events` table.

The `order_id` column is the primary key. The listener uses an insert operation with `ON CONFLICT (order_id) DO NOTHING` to prevent a duplicate event from creating another processing record.

This makes repeated deliveries of the same order event safe for the processing step represented by this table.

## Testing

The project includes:

* Unit tests for successful event processing.
* Unit tests for duplicate event handling.
* A PostgreSQL integration test that verifies repeated delivery of the same event does not create duplicate records.
* A configuration test that verifies the expected local profile and RabbitMQ retry properties.

Run the test suite from the project root:

```powershell
.\mvnw.cmd test
```

If the project does not include the Maven Wrapper, use:

```powershell
mvn test
```

The PostgreSQL integration test uses Testcontainers and requires a working Docker environment.

## Local Development

1. Install Java 21.

2. Install Docker Desktop and ensure its Docker engine is running.

3. Configure PostgreSQL and the application's local database settings.

4. Start RabbitMQ with the management interface enabled, if it is not already running:

   ```powershell
   docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4-management
   ```

5. Configure the required RabbitMQ connection settings in your local application configuration.

6. Start the Inventory API from IntelliJ IDEA or Maven.

RabbitMQ Management UI: http://localhost:15672

The default credentials for a fresh local RabbitMQ container are `guest` / `guest`. Use appropriate credentials and access controls outside local development.

## Important Notes

* A dead-letter queue receives messages only when the main queue's dead-letter exchange and routing configuration are set up correctly.
* PostgreSQL idempotency prevents duplicate processing records; it does not automatically make every possible business side effect idempotent.
* The current event publication flow should be strengthened with a transactional outbox if reliable delivery between database commits and RabbitMQ publishing is required in production.
