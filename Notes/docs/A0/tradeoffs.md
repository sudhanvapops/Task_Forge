### A0

Compose for the dev DB, 
Testcontainers for tests: why both, and why not local Postgres. 
Reproducible DB, with the same pinned version in both.
“what I’d do differently”: assert current_database() in the smoke test so the check is automatic instead of log-reading.