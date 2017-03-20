curl -v  -s -X POST $OS_AUTH_URL/tokens -H "Content-Type: application/json" -d '{"auth": {"tenantName": "'"$OS_TENANT_NAME"'", "passwordCredentials": {"username": "'"$OS_USERNAME"'", "password": 
"'"$OS_PASSWORD"'"}}}' | python3 -m json.tool >> data.json
