#!/bin/bash

# Run Separate Services Architecture Locally
# Starts Flowable standalone + Interaction Service

set -e

echo "🚀 Starting Separate Services Architecture"

# Function to check if port is available
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null ; then
        echo "❌ Port $port is already in use"
        return 1
    fi
    return 0
}

# Check if required ports are available
echo "🔍 Checking port availability..."
check_port 5432 || exit 1  # PostgreSQL
check_port 9092 || exit 1  # Kafka
check_port 8080 || exit 1  # Flowable UI
check_port 8081 || exit 1  # Interaction Service

# Start PostgreSQL databases
echo "🐘 Starting PostgreSQL databases..."
docker run -d --name flowable-postgres \
  -e POSTGRES_DB=flowable \
  -e POSTGRES_USER=flowable \
  -e POSTGRES_PASSWORD=flowable \
  -p 5432:5432 \
  postgres:13

docker run -d --name app-postgres \
  -e POSTGRES_DB=interaction_service \
  -e POSTGRES_USER=app_user \
  -e POSTGRES_PASSWORD=app_password \
  -p 5433:5432 \
  postgres:13

# Wait for PostgreSQL to be ready
echo "⏳ Waiting for PostgreSQL..."
sleep 10

# Start Kafka
echo "📨 Starting Kafka..."
docker run -d --name kafka \
  -p 9092:9092 \
  -e KAFKA_CFG_NODE_ID=0 \
  -e KAFKA_CFG_PROCESS_ROLES=controller,broker \
  -e KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=0@kafka:9093 \
  -e KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
  -e KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
  -e KAFKA_CFG_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e KAFKA_CFG_INTER_BROKER_LISTENER_NAME=PLAINTEXT \
  -e KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE=true \
  bitnami/kafka:3.5

# Wait for Kafka to be ready
echo "⏳ Waiting for Kafka..."
sleep 15

# Start Flowable standalone
echo "🔄 Starting Flowable Platform..."
docker run -d --name flowable-platform \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/flowable \
  -e SPRING_DATASOURCE_USERNAME=flowable \
  -e SPRING_DATASOURCE_PASSWORD=flowable \
  -e FLOWABLE_COMMON_APP_IDM-ADMIN_USER=admin \
  -e FLOWABLE_COMMON_APP_IDM-ADMIN_PASSWORD=test \
  -v $(pwd)/bpmn-processes:/opt/flowable/work/ \
  flowable/flowable-ui:7.0.1

# Wait for Flowable to be ready
echo "⏳ Waiting for Flowable Platform..."
sleep 30

# Start Interaction Service
echo "🚀 Starting Interaction Service..."
SPRING_PROFILES_ACTIVE=separate-services ./mvnw spring-boot:run &
INTERACTION_PID=$!

# Wait for services to be ready
echo "⏳ Waiting for services to start..."
sleep 20

echo "✅ Separate Services Architecture Started!"
echo ""
echo "🌐 Service Endpoints:"
echo "  📊 Flowable UI:     http://localhost:8080/flowable-task"
echo "  🔧 Flowable REST:   http://localhost:8080/flowable-rest"
echo "  🚀 App Service:     http://localhost:8081"
echo ""
echo "🗄️  Databases:"
echo "  📈 Flowable DB:     localhost:5432/flowable"
echo "  💾 App DB:          localhost:5433/interaction_service"
echo "  📨 Kafka:           localhost:9092"
echo ""
echo "🛑 To stop: ./stop-separate-services.sh"

# Keep script running
wait $INTERACTION_PID
