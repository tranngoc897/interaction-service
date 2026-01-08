#!/bin/bash

echo "================================================="
echo "   AUTOMATED ONBOARDING TEST SCRIPT"
echo "================================================="

# Base URL
URL="http://localhost:8081/api/onboarding"

# 1. Start Onboarding
echo ""
echo "[Step 1] Starting Onboarding..."
RESPONSE=$(curl -s -X POST "$URL/start?userId=auto-tester-$(date +%s)")
echo "Response: $RESPONSE"

# 2. Extract Instance ID
# Using grep and cut to work without jq
INSTANCE_ID=$(echo $RESPONSE | grep -o '"instanceId":"[^"]*"' | cut -d'"' -f4)

if [ -z "$INSTANCE_ID" ] || [ "$INSTANCE_ID" == "null" ]; then
  echo "❌ Error: Could not get Instance ID. Please make sure the server is running on port 8081."
  exit 1
fi

echo "✅ Created Instance ID: $INSTANCE_ID"

# Function to check status
check_status() {
    echo ""
    echo "[Info] Checking Status..."
    STATUS_RES=$(curl -s -X GET "$URL/$INSTANCE_ID/status")
    STATE=$(echo $STATUS_RES | grep -o '"currentState":"[^"]*"' | cut -d'"' -f4)
    echo "Current State: $STATE"
}

# Function to perform action
perform_action() {
    ACTION=$1
    echo ""
    echo "[Action] Sending '$ACTION'..."
    curl -s -X POST "$URL/$INSTANCE_ID/action" \
         -H "Content-Type: application/json" \
         -d "{ \"action\": \"$ACTION\" }" > /dev/null
    
    # Check result
    check_status
}

# 3. Execute Flow
check_status

# PHONE_ENTERED -> NEXT -> OTP_VERIFIED
perform_action "NEXT"

# OTP_VERIFIED -> NEXT -> PROFILE_COMPLETED
perform_action "NEXT"

# PROFILE_COMPLETED -> NEXT -> DOC_UPLOADED
perform_action "NEXT"

# DOC_UPLOADED -> NEXT -> EKYC_PENDING (Async Start)
perform_action "NEXT"

echo ""
echo "⏳ Waiting for Async Processing (Simulation)..."
echo "In a real environment, Kafka consumers would process this."
echo "You can check the logs to see if EkycCallbackConsumer is triggered."

echo ""
echo "Done! Test script finished."
