curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "new_user",
    "email": "new_user@aktiia.com",
    "password": "password"
  }'