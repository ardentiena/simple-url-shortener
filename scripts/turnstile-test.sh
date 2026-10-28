#!/usr/bin/env bash
# Pemakaian: ./scripts/turnstile-test.sh body-expiry untuk secret key selalu lolos
#            ./scripts/turnstile-test.sh rejected untuk secret key selalu gagal
TEST_MODE="$1"
BASE_URL="http://localhost:8081"
TURNSTILE_TOKEN_HEADER="X-Turnstile-Token"
DUMMY_TURNSTILE_TOKEN="XXXX.DUMMY.TOKEN.XXXX"
TARGET_LONG_URL="https://spring.io"
RUN_ID=$(date +%s)

post_link_status() {
  local alias_suffix="$1"
  shift
  curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/links" \
    -H "Content-Type: application/json" "$@" \
    -d "{\"longUrl\":\"$TARGET_LONG_URL\",\"alias\":\"tt${RUN_ID}${alias_suffix}\"}"
}

post_link_full_response() {
  local alias_suffix="$1"
  shift
  curl -s -i -X POST "$BASE_URL/api/links" \
    -H "Content-Type: application/json" "$@" \
    -d "{\"longUrl\":\"$TARGET_LONG_URL\",\"alias\":\"tt${RUN_ID}${alias_suffix}\"}"
  echo
}

echo "Request 1 sampai 5 tanpa token, harus 201"
for request_number in $(seq 1 5); do
  echo "request ${request_number}: $(post_link_status "a${request_number}")"
done

echo
echo "Request 6 tanpa token, respons lengkap, harus 403 CAPTCHA diperlukan"
post_link_full_response "a6"

if [ "$TEST_MODE" = "rejected" ]; then
  echo "Request 7 dengan token dummy, respons lengkap, harus 403 token ditolak"
  post_link_full_response "a7" -H "$TURNSTILE_TOKEN_HEADER: $DUMMY_TURNSTILE_TOKEN"
  exit 0
fi

echo "Request 7 sampai 10 dengan token, harus 201"
for request_number in $(seq 7 10); do
  echo "request ${request_number}: $(post_link_status "a${request_number}" -H "$TURNSTILE_TOKEN_HEADER: $DUMMY_TURNSTILE_TOKEN")"
done

echo
echo "Request 11 dengan token, respons lengkap, harus 429"
post_link_full_response "a11" -H "$TURNSTILE_TOKEN_HEADER: $DUMMY_TURNSTILE_TOKEN"

echo "Menunggu 70 detik tanpa request apa pun"
sleep 70
probe_status=$(post_link_status "p1")
echo "Probe 1 setelah 70 detik: ${probe_status}"

if [ "$probe_status" = "429" ]; then
  echo "Masih 429, menunggu 230 detik lagi tanpa request apa pun"
  sleep 230
  echo "Probe 2 setelah 300 detik: $(post_link_status "p2")"
fi