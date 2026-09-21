#!/bin/sh
set -e

echo "==> DigitalChopda.in Backend starting..."

# Retry loop for database migrations
echo "==> Running database migrations..."
MAX_RETRIES=30
RETRY_COUNT=0

until npm run migrate || [ $RETRY_COUNT -eq $MAX_RETRIES ]; do
  echo "Database not ready yet... retrying in 2 seconds ($RETRY_COUNT/$MAX_RETRIES)"
  RETRY_COUNT=$((RETRY_COUNT + 1))
  sleep 2
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
  echo "❌ Error: Failed to connect to database and apply migrations after $MAX_RETRIES attempts."
  exit 1
fi

echo "==> Starting backend server..."
exec dumb-init node src/index.js
