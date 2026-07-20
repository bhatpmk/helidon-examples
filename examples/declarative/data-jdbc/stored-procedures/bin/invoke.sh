#!/usr/bin/env bash
set -euo pipefail

base_url="${BASE_URL:-http://localhost:8080}"

echo "Reserving order 1001 with IN, INOUT, and OUT values:"
curl --fail-with-body -sS -X POST "${base_url}/orders/1001/reserve" \
    -H 'Content-Type: application/json' \
    -d '{"customerId":501,"requestedBy":"fulfillment-service","attempts":0}'
echo

echo "Calculating the priority reservation fee through a MySQL function:"
curl --fail-with-body -sS "${base_url}/orders/1001/reservation-fee/true"
echo

echo "Reading available lines for order 1001 from a direct procedure result set:"
curl --fail-with-body -sS "${base_url}/orders/1001/lines/false"
echo

echo "Reading backordered lines for order 1002 from the same procedure:"
curl --fail-with-body -sS "${base_url}/orders/1002/lines/true"
echo
