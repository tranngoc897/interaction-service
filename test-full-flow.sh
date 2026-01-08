#!/bin/bash

echo "================================================="
echo "   AUTOMATED ONBOARDING TEST SCRIPT (WITH ASYNC)"
echo "================================================="

# Base URL
URL="http://localhost:8081/api/onboarding"
SIM_URL="http://localhost:8081/api/simulation"

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
    return 0
}

# Function to perform action
perform_action() {
    ACTION=$1
    echo ""
    echo "[Action] Sending '$ACTION'..."
    ACTION_RES=$(curl -s -X POST "$URL/$INSTANCE_ID/action" \
         -H "Content-Type: application/json" \
         -d "{ \"action\": \"$ACTION\" }")
    echo "Action Result: $ACTION_RES"
    
    # Check result
    check_status
}

# 3. Execute Sync Flow
check_status

# PHONE_ENTERED -> NEXT -> OTP_VERIFIED
perform_action "NEXT"

# OTP_VERIFIED -> NEXT -> PROFILE_COMPLETED
perform_action "NEXT"

# PROFILE_COMPLETED -> NEXT -> DOC_UPLOADED
perform_action "NEXT"

# DOC_UPLOADED -> NEXT -> EKYC_PENDING (Async Start)
perform_action "NEXT"


# 4. Simulate eKYC Callback
echo ""
echo "⏳ Waiting 2 seconds before verifying eKYC..."
sleep 2

echo "🤖 Simulating eKYC Approval (via Kafka)..."
curl -s -X POST "$SIM_URL/ekyc-callback?instanceId=$INSTANCE_ID&result=APPROVED&score=0.95"
echo ""

# Wait for processing
sleep 2
check_status
# Should be AML_PENDING (Auto-progressed from EKYC_APPROVED)

# 5. Simulate AML Callback
STATE=$(curl -s -X GET "$URL/$INSTANCE_ID/status" | grep -o '"currentState":"[^"]*"' | cut -d'"' -f4)
if [ "$STATE" == "AML_PENDING" ]; then
    echo "🤖 Simulating AML Clearance (via Kafka)..."
    curl -s -X POST "$SIM_URL/aml-callback?instanceId=$INSTANCE_ID&result=CLEAR"
    echo ""
    
    sleep 2
    check_status
    # Should be ACCOUNT_CREATED (Auto-progressed from AML_CLEARED)
    
    # Final step
    perform_action "NEXT"
    echo "🎉 Flow Completed!"
else
    echo "⚠️ Unexpected state: $STATE. Expected AML_PENDING."
fi
