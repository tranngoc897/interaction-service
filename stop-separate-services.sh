#!/bin/bash

# Stop Separate Services Architecture
echo "🛑 Stopping Separate Services Architecture..."

# Stop Spring Boot app
echo "Stopping Interaction Service..."
pkill -f "spring-boot:run" || echo "No Spring Boot process found"

# Stop Docker containers
echo "Stopping Docker containers..."
docker stop flowable-platform kafka app-postgres flowable-postgres 2>/dev/null || echo "Some containers not running"

# Remove Docker containers
echo "Removing Docker containers..."
docker rm flowable-platform kafka app-postgres flowable-postgres 2>/dev/null || echo "Some containers not found"

echo "✅ All services stopped and cleaned up!"
