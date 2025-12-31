# Kafka & Camunda Event Integration Guide

This guide explains how to run the integrated environment and how the event-driven architecture works.

## 1. Prerequisites
- Docker & Docker Compose installed
- Java 21 & Maven 3.9+

## 2. Infrastructure Setup
Run the following command to start Postgres, Kafka (with Zookeeper), and Kafdrop:

```bash
docker-compose up -d
```

Services started:
- **Postgres**: `localhost:5433` (DB: `ob_camunda`, User: `postgres`, Pass: `admin`)
- **Kafka**: `localhost:29092` (Internal: `kafka:9092`)
- **Kafdrop (Kafka UI)**: `http://localhost:9000`
- **Camunda (Optional Standalone)**: `http://localhost:8080/camunda`

## 3. Application Setup
Ensure `src/main/resources/application.yaml` is pointing to the correct ports:
- Datasource: `localhost:5433`
- Kafka: `localhost:29092`

Run the application:
```bash
./mvnw spring-boot:run
```

## 4. Key Event Features

### A. Camunda Kafka Plugin (`CamundaKafkaEventPlugin`)
This plugin is automatically registered in Spring Boot. It hooks into the Camunda History Event system and:
1. Captures **Process Instance Start/End**.
2. Captures **Activity Start/End** (Service Tasks, User Tasks, etc.).
3. Publishes these as events to Kafka via `WorkflowEventPublisher`.

### B. Automated History Tracking (`WorkflowHistoryEventListener`)
This listener listens to **all** internal Spring Events and persists them to the `workflow_history` table:
- `WorkflowStateEvent` (BPMN state changes)
- `MilestoneEvent` (Business milestones)
- `ComplianceEvent` (AML/KYC results)
- `InteractionStepEvent` (User UI steps)

### C. Kafka Consumption (`KafkaEventListener`)
Demonstrates how distributed services can react to these events. You can see logs in the application console when events are received via Kafka.

## 5. Verification Steps
1. Open **Kafdrop** (`http://localhost:9000`) to see the topics: `workflow-state-events`, `milestone-events`, `compliance-events`, etc.
2. Trigger a process via Camunda or API.
3. Check the `workflow_history` table in the database to see the automated audit trail.
4. Observe the console logs for "Received Kafka Event..." messages.

## 6. Docker Deployment (Optional)
To build and run the entire stack including the application in Docker:
```bash
docker build -t interaction-service .
# Then add it to docker-compose.yml or run manually
```
