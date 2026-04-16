#!/usr/bin/env node

import express from "express";
import { readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const host = process.env.HOST ?? "127.0.0.1";
const port = Number.parseInt(process.env.PORT ?? "18081", 10);
const usersFile = process.env.MOCK_USERS_FILE
  ? path.resolve(process.cwd(), process.env.MOCK_USERS_FILE)
  : path.join(__dirname, "users.json");
const app = express();

function loadUsers() {
  const parsed = JSON.parse(readFileSync(usersFile, "utf8"));
  if (!Array.isArray(parsed)) {
    throw new Error(`Expected JSON array in ${usersFile}`);
  }
  return parsed;
}

app.use((req, _res, next) => {
  console.log(`${new Date().toISOString()} ${req.method} ${req.path}`);
  next();
});

app.get(["/info/users", "/api/users"], (_req, res) => {
  const users = loadUsers();
  console.log(`${new Date().toISOString()} serving ${users.length} users from ${usersFile}`);
  res.json(users);
});

app.get("/health", (_req, res) => {
  res.json({ status: "ok", usersFile });
});

app.use((_req, res) => {
  res.status(404).json({
    error: "not_found",
    supportedPaths: ["/info/users", "/api/users", "/health"],
  });
});

app.listen(port, host, () => {
  console.log(`Mock nsoft server listening on http://${host}:${port}`);
  console.log(`Serving users from ${usersFile}`);
});
