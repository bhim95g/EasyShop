# EasyShop

EasyShop is a Java 17 / Spring Boot 2.7 REST API for an online store with products, categories, JWT authentication and shopping carts. The Maven application lives in `capstone-starter/`.

## Customer profile API

Authenticated customers can view and update **their own** profile. No user ID is accepted in the URL or request body.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/profile` | View the current user's profile |
| `PUT` | `/profile` | Replace the current user's profile fields |

Both endpoints require an `Authorization: Bearer <JWT>` header obtained from `POST /login`. Clients cannot read or update another user's profile using this API.

### View a profile

```bash
curl -H "Authorization: Bearer YOUR_TOKEN" http://localhost:8080/profile
```

### Update a profile

`PUT /profile` is a **full replacement** for the eight editable profile fields. Include all fields in the request body, even fields that should be blank. The server obtains the user ID from the authenticated user and ignores attempts to supply it in the JSON.

```bash
curl -X PUT http://localhost:8080/profile \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Alice",
    "lastName": "Example",
    "phone": "412-555-0100",
    "email": "alice@example.com",
    "address": "123 Main Street",
    "city": "Pittsburgh",
    "state": "PA",
    "zip": "15222"
  }'
```

Expected response codes:

- `200 OK`: Profile retrieved or updated.
- `400 Bad Request`: Missing required fields, invalid email or fields longer than the database limits.
- `401 Unauthorized`: Missing or invalid authentication.
- `404 Not Found`: Profile record does not exist for the authenticated user.
- `500 Internal Server Error`: Unexpected server/database error.

### Running the targeted tests

Install Java 17 and Maven, then:

```bash
cd capstone-starter
mvn -Dtest=ProfileControllerTest,MySqlProfileDaoUnitTest,ProfileUpdateRequestTest test
```

The targeted tests use mocks and validation, without requiring a local MySQL instance. The pre-existing integration tests do depend on a database.

### Configuration warning

The repository's existing `capstone-starter/src/main/resources/application.properties` includes committed database/JWT configuration values. If any are real, treat them as exposed, rotate them, and move secrets to environment variables or a secrets manager. Removing secrets from a new commit does not remove old Git history.
