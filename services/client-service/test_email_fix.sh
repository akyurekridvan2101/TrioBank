#!/bin/bash
# Test script to verify client email update bug fix
# This script tests that email is preserved when updating only phone and address

set -e

API_URL="${API_URL:-http://localhost:8081}"
CLIENT_ID=""
ORIGINAL_EMAIL="test-email-$(date +%s)@example.com"
ORIGINAL_GSM="5321234567"
NEW_GSM="5329999999"
NEW_ADDRESS="Test Street, Istanbul"

echo "========================================="
echo "Client Email Update Bug Fix - Test"
echo "========================================="
echo ""

# Step 1: Create a test client
echo "Step 1: Creating test client..."
CREATE_RESPONSE=$(curl -s -X POST "${API_URL}/clients" \
  -H "Content-Type: application/json" \
  -d "{
    \"user_id\": \"$(uuidgen)\",
    \"tc_no\": \"12345678901\",
    \"first_name\": \"Test\",
    \"last_name\": \"User\",
    \"email\": \"${ORIGINAL_EMAIL}\",
    \"gsm\": \"${ORIGINAL_GSM}\",
    \"birth_date\": \"1990-01-01T00:00:00Z\",
    \"address\": {
      \"street\": \"Original Street\",
      \"city\": \"Ankara\",
      \"district\": \"Çankaya\",
      \"postal_code\": \"06000\"
    }
  }")

CLIENT_ID=$(echo "$CREATE_RESPONSE" | jq -r '.id')

if [ -z "$CLIENT_ID" ] || [ "$CLIENT_ID" = "null" ]; then
  echo "❌ FAILED: Could not create client"
  echo "Response: $CREATE_RESPONSE"
  exit 1
fi

echo "✓ Client created successfully"
echo "  Client ID: $CLIENT_ID"
echo "  Email: $ORIGINAL_EMAIL"
echo "  GSM: $ORIGINAL_GSM"
echo ""

# Step 2: Verify initial state
echo "Step 2: Verifying initial state..."
INITIAL_STATE=$(curl -s -X GET "${API_URL}/clients/${CLIENT_ID}")
INITIAL_EMAIL=$(echo "$INITIAL_STATE" | jq -r '.email')
INITIAL_GSM=$(echo "$INITIAL_STATE" | jq -r '.gsm')

if [ "$INITIAL_EMAIL" != "$ORIGINAL_EMAIL" ]; then
  echo "❌ FAILED: Initial email mismatch"
  echo "  Expected: $ORIGINAL_EMAIL"
  echo "  Got: $INITIAL_EMAIL"
  exit 1
fi

echo "✓ Initial state verified"
echo "  Email: $INITIAL_EMAIL"
echo "  GSM: $INITIAL_GSM"
echo ""

# Step 3: Update only phone and address (WITHOUT email in payload)
echo "Step 3: Updating only phone and address (email NOT in payload)..."
echo "  New GSM: $NEW_GSM"
echo "  New Address: $NEW_ADDRESS"
UPDATE_RESPONSE=$(curl -s -X PUT "${API_URL}/clients/${CLIENT_ID}" \
  -H "Content-Type: application/json" \
  -d "{
    \"gsm\": \"${NEW_GSM}\",
    \"address\": {
      \"street\": \"${NEW_ADDRESS}\"
    }
  }")

echo "✓ Update request sent"
echo ""

# Step 4: Verify email is preserved
echo "Step 4: Verifying email is PRESERVED after update..."
FINAL_STATE=$(curl -s -X GET "${API_URL}/clients/${CLIENT_ID}")
FINAL_EMAIL=$(echo "$FINAL_STATE" | jq -r '.email')
FINAL_GSM=$(echo "$FINAL_STATE" | jq -r '.gsm')
FINAL_ADDRESS=$(echo "$FINAL_STATE" | jq -r '.address.street')

echo "  Email: $FINAL_EMAIL"
echo "  GSM: $FINAL_GSM"
echo "  Address: $FINAL_ADDRESS"
echo ""

# Validate results
PASS=true

if [ "$FINAL_EMAIL" != "$ORIGINAL_EMAIL" ]; then
  echo "❌ FAILED: Email was NOT preserved!"
  echo "  Expected: $ORIGINAL_EMAIL"
  echo "  Got: $FINAL_EMAIL"
  PASS=false
else
  echo "✓ SUCCESS: Email preserved correctly!"
fi

if [ "$FINAL_GSM" != "$NEW_GSM" ]; then
  echo "❌ FAILED: GSM was not updated!"
  echo "  Expected: $NEW_GSM"
  echo "  Got: $FINAL_GSM"
  PASS=false
else
  echo "✓ SUCCESS: GSM updated correctly!"
fi

if [ "$FINAL_ADDRESS" != "$NEW_ADDRESS" ]; then
  echo "❌ WARNING: Address was not updated as expected"
  echo "  Expected: $NEW_ADDRESS"
  echo "  Got: $FINAL_ADDRESS"
fi

echo ""
echo "========================================="
if [ "$PASS" = true ]; then
  echo "✓✓✓ ALL TESTS PASSED ✓✓✓"
  echo "========================================="
  exit 0
else
  echo "❌❌❌ TESTS FAILED ❌❌❌"
  echo "========================================="
  exit 1
fi
