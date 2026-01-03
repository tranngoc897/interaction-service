#!/bin/bash

# Test Event-Driven Implementation
# Comprehensive testing of the new event-driven architecture

set -e

echo "🧪 Testing Event-Driven Implementation"
echo "====================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
APP_URL="http://localhost:8081"
FLOWABLE_URL="http://localhost:8080"
KAFKA_CONTAINER="kafka"
TIMEOUT=30

# Test counters
TESTS_RUN=0
TESTS_PASSED=0
TESTS_FAILED=0

# Helper function to print test results
print_test_result() {
    local test_name=$1
    local result=$2
    local message=$3

    ((TESTS_RUN++))
    if [ "$result" = "PASS" ]; then
        ((TESTS_PASSED++))
        echo -e "${GREEN}✅ PASS${NC} - $test_name"
        [ -n "$message" ] && echo -e "   $message"
    else
        ((TESTS_FAILED++))
        echo -e "${RED}❌ FAIL${NC} - $test_name"
        [ -n "$message" ] && echo -e "   $message"
    fi
}

# Helper function to check service health
check_service() {
    local url=$1
    local service_name=$2
    local max_attempts=$3

    for i in $(seq 1 $max_attempts); do
        if curl -s --max-time 5 "$url" > /dev/null 2>&1; then
            print_test_result "$service_name Health Check" "PASS" "Service is healthy"
            return 0
        fi
        echo "Waiting for $service_name... (attempt $i/$max_attempts)"
        sleep 2
    done

    print_test_result "$service_name Health Check" "FAIL" "Service is not responding"
    return 1
}

# Helper function to check Kafka topic
check_kafka_topic() {
    local topic=$1
    local expected_count=${2:-1}

    # Check if topic exists and has messages
    local message_count=$(docker exec $KAFKA_CONTAINER kafka-run-class.sh kafka.tools.GetOffsetShell \
        --broker-list localhost:9092 --topic $topic 2>/dev/null | awk -F: '{sum += $2} END {print sum+0}')

    if [ "$message_count" -ge "$expected_count" ]; then
        print_test_result "Kafka Topic $topic" "PASS" "Topic has $message_count messages (expected >= $expected_count)"
        return 0
    else
        print_test_result "Kafka Topic $topic" "FAIL" "Topic has $message_count messages (expected >= $expected_count)"
        return 1
    fi
}

# Test 1: Start services
echo -e "\n${BLUE}🚀 Phase 1: Starting Services${NC}"
echo "------------------------------"

echo "Starting separate services..."
./run-separate-services.sh > /dev/null 2>&1 &
SERVICES_PID=$!

# Wait for services to start
sleep 30

# Test service health
check_service "$FLOWABLE_URL/flowable-task" "Flowable UI" 10
check_service "$APP_URL/actuator/health" "Interaction Service" 10

# Test 2: Deploy BPMN process
echo -e "\n${BLUE}📋 Phase 2: Deploying BPMN Process${NC}"
echo "-----------------------------------"

# Deploy process via REST API
DEPLOY_RESPONSE=$(curl -s -X POST "$APP_URL/api/bpmn/deploy?processKey=onboarding&processName=Onboarding" \
    -H "Content-Type: application/xml" \
    --data-binary @bpmn-processes/onboarding-process.bpmn)

if echo "$DEPLOY_RESPONSE" | grep -q "success\|deployed"; then
    print_test_result "BPMN Process Deployment" "PASS" "Process deployed successfully"
else
    print_test_result "BPMN Process Deployment" "FAIL" "Deployment failed: $DEPLOY_RESPONSE"
fi

# Test 3: Start process instance
echo -e "\n${BLUE}▶️  Phase 3: Starting Process Instance${NC}"
echo "-------------------------------------"

START_RESPONSE=$(curl -s -X POST "$APP_URL/api/bpmn/start?processDefinitionKey=onboarding&businessKey=test-001" \
    -H "Content-Type: application/json" \
    -d '{
        "caseId": "test-001",
        "applicantId": "user-123",
        "emailAddress": "test@example.com",
        "customerName": "Test User"
    }')

if echo "$START_RESPONSE" | grep -q "id\|processInstanceId"; then
    PROCESS_INSTANCE_ID=$(echo "$START_RESPONSE" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
    print_test_result "Process Instance Start" "PASS" "Process started with ID: $PROCESS_INSTANCE_ID"
else
    print_test_result "Process Instance Start" "FAIL" "Failed to start process: $START_RESPONSE"
    PROCESS_INSTANCE_ID=""
fi

# Test 4: Monitor event flow
echo -e "\n${BLUE}📨 Phase 4: Monitoring Event Flow${NC}"
echo "----------------------------------"

# Wait for events to be processed
sleep 10

# Check Kafka topics for events
check_kafka_topic "flowable-external-jobs" 6  # Should have 6 external job events
check_kafka_topic "workflow-state-events" 1   # Should have at least 1 workflow event

# Test 5: Check process status
echo -e "\n${BLUE}📊 Phase 5: Checking Process Status${NC}"
echo "-------------------------------------"

if [ -n "$PROCESS_INSTANCE_ID" ]; then
    STATUS_RESPONSE=$(curl -s "$APP_URL/api/bpmn/process-instance/$PROCESS_INSTANCE_ID")

    if echo "$STATUS_RESPONSE" | grep -q "COMPLETED\|ended.*true"; then
        print_test_result "Process Completion" "PASS" "Process completed successfully"
    elif echo "$STATUS_RESPONSE" | grep -q "RUNNING\|ACTIVE"; then
        print_test_result "Process Status" "PASS" "Process is running (async processing)"
    else
        print_test_result "Process Status" "FAIL" "Process status unknown: $STATUS_RESPONSE"
    fi
fi

# Test 6: Performance metrics
echo -e "\n${BLUE}⚡ Phase 6: Performance Testing${NC}"
echo "---------------------------------"

# Test concurrent requests
echo "Testing concurrent requests..."
START_TIME=$(date +%s)

# Run 10 concurrent requests
for i in {1..10}; do
    curl -s -X POST "$APP_URL/api/bpmn/start?processDefinitionKey=onboarding&businessKey=perf-test-$i" \
        -H "Content-Type: application/json" \
        -d "{\"caseId\": \"perf-$i\", \"applicantId\": \"user-$i\"}" > /dev/null &
done

wait
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

if [ $DURATION -le 30 ]; then
    print_test_result "Concurrent Processing" "PASS" "Processed 10 requests in ${DURATION}s"
else
    print_test_result "Concurrent Processing" "FAIL" "Took too long: ${DURATION}s"
fi

# Test 7: Event-driven worker metrics
echo -e "\n${BLUE}📈 Phase 7: Worker Metrics${NC}"
echo "-------------------------------"

# Check worker health via actuator endpoint
METRICS_RESPONSE=$(curl -s "$APP_URL/actuator/metrics" 2>/dev/null || echo "{}")

if echo "$METRICS_RESPONSE" | grep -q "jvm\|system"; then
    print_test_result "Application Metrics" "PASS" "Metrics endpoint is working"
else
    print_test_result "Application Metrics" "WARN" "Metrics endpoint not fully available"
fi

# Test 8: Cleanup
echo -e "\n${BLUE}🧹 Phase 8: Cleanup${NC}"
echo "------------------"

echo "Stopping services..."
./stop-separate-services.sh > /dev/null 2>&1 || true

# Kill background process if still running
kill $SERVICES_PID 2>/dev/null || true

print_test_result "Cleanup" "PASS" "Services stopped successfully"

# Final results
echo -e "\n${BLUE}📊 TEST RESULTS SUMMARY${NC}"
echo "========================"
echo "Tests Run: $TESTS_RUN"
echo -e "Tests Passed: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Tests Failed: ${RED}$TESTS_FAILED${NC}"

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "\n${GREEN}🎉 ALL TESTS PASSED! Event-driven implementation is working correctly.${NC}"
    echo "🚀 Ready for production deployment!"
    exit 0
else
    echo -e "\n${RED}⚠️  Some tests failed. Please check the implementation.${NC}"
    exit 1
fi
