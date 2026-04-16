# Mock Nsoft Server

Small local server for testing `TermxUserProvider` without a real `nsoft` dependency.

## Run

```bash
cd /job/helex/htx/lmb-terminology-server
cd tools/mock-nsoft && npm install
cd ../..
node tools/mock-nsoft/server.mjs
```

By default it listens on `http://127.0.0.1:18081` and serves `tools/mock-nsoft/users.json`.

## Custom data

```bash
MOCK_USERS_FILE=/absolute/path/to/users.json PORT=18081 node tools/mock-nsoft/server.mjs
```

Expected JSON shape:

```json
[
  {
    "id": 1,
    "firstname": "Alice",
    "lastname": "Admin",
    "email": "alice@example.org",
    "permissions": ["*.*.*"]
  }
]
```

## Endpoints

- `GET /info/users`
- `GET /api/users`
- `GET /health`

## Wiring Into TermX

```bash
cd /job/helex/htx/lmb-terminology-server
NSOFT_URL=http://127.0.0.1:18081 ./gradlew :termx-app:run -Pdev
```

Then query:

```bash
curl -H 'Authorization: Bearer yupi' http://127.0.0.1:8200/api/users
```
