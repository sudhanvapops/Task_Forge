# PHASE 1 — SQL & psql Companion
### Learn only the SQL that each TaskForge block forces you to write

> **Companion to:** `Topic.md` (Blocks A–G). Same structure, same rules.
> **Budget:** ~14–16 hours total, spread across the 4–5 weeks. Most drills are 20–60 minutes.
> **Goal:** go from "I ask an LLM and paste" to "I can write the claim query from a blank file and explain every clause."

---

## 0. How to use this file

1. Each **SQL block** matches a TaskForge block. Do the SQL block **just before or during** its TaskForge block, not after.
2. Every query starts life in **`psql`**, against dummy rows, *before* it goes into Java.
3. Core queries (claim, heartbeat, reaper, completion, idempotent insert) are **native SQL** via `JdbcTemplate` or `@Query(nativeQuery = true)`. Hibernate hides exactly what you're learning. JPA is fine for plain CRUD elsewhere.
4. Micro-tasks are 20–60 min. Prefix `S-` so they don't clash with `Topic.md` IDs (`S-C3` = SQL task 3 for Block C).
5. **Closed-book check** = on paper, no notes, no AI.
6. Every query you struggle with goes into `sql/NOTES.md`: *the query, what I expected, what happened, the fix.*

### Rules for using AI on SQL

| Do | Don't |
|---|---|
| Write your attempt first, even if wrong | "Write me a query that claims a job" |
| "Here's my query and what I expected. What's wrong? Hint only." | Paste an error and apply the fix unread |
| Ask it to explain *one clause* you don't get | Ask for the finished query for any core path |
| If you do receive a query: explain each clause aloud, close it, rewrite it tomorrow | Commit a query you can't explain |

### Anti-loop rules

| Situation | Rule |
|---|---|
| Query errors for > **45 min** | Strip it to the smallest query that fails. Test each piece alone. |
| Don't understand a clause | Run it on 3 dummy rows and look at the output. |
| Tempted by window functions, triggers, stored procs | `parking-lot.md`. Not this phase. |
| Behind schedule | Cut the *optional* tasks (marked ⭐), never the closed-book checks. |

---

## 1. The map

```
SQL-A  psql + basic queries           (SELECT/INSERT/UPDATE/DELETE, JOIN)     ~4h   → with Block A
   ↓
SQL-B  Schema, constraints, txns      (DDL, CHECK/UNIQUE, BEGIN/COMMIT)       ~3h   → with Block B
   ↓
SQL-C  Locking & claiming             (FOR UPDATE, SKIP LOCKED, CTE+UPDATE)   ~5h   → with Block C  ← the core
   ↓
SQL-D  Time, retries, moving rows     (intervals, INSERT…SELECT, DELETE…RETURNING) ~2.5h → with Block D
   ↓
SQL-E  Upserts & idempotency          (ON CONFLICT)                           ~2h   → with Block E
   ↓
SQL-F  Plans & indexes                (EXPLAIN ANALYZE, partial index)        ~3h   → with Block F
   ↓
SQL-G  Metrics queries                (percentiles, pg_stat_*)                ~1.5h → with Block G
```

### What you will NOT learn in Phase 1 (global)

| Skipped | Why |
|---|---|
| Window functions, recursive CTEs | No block needs them |
| Stored procedures, triggers, views, materialized views | Logic stays in Java; keep the DB dumb |
| Partitioning, replication, `VACUUM` tuning | Scale concerns. Know that heavy `UPDATE` churn creates bloat, nothing more. |
| `LISTEN/NOTIFY`, advisory locks | Optimization; `SKIP LOCKED` is enough |
| Query planner internals, join algorithms in depth | Read plans, don't re-derive them |
| ORM mapping details | You already have those from Phase 0 |

### Folder to add to the repo

```
sql/
├── scratch/          # one file per block: a-basics.sql, b-schema.sql, c-claim.sql ...
├── queries/          # the final hot-path queries you actually use
└── NOTES.md          # struggles, fixes, "things that surprised me"
```

---

# SQL-A — psql & Basic Queries
**Use with Block A · Time box: ~4 hours**

### Goal
Be comfortable living in `psql` and read/write simple queries without help.

### Covers
- **psql basics**: connecting to your Docker Postgres, `\l`, `\dt`, `\d table`, `\x`, `\timing`, `\q`
- **Data types you'll use**: `bigint`/`uuid`, `text`, `integer`, `boolean`, `timestamptz`, `jsonb`
- **`SELECT`** with `WHERE`, `ORDER BY`, `LIMIT`
- **`INSERT`, `UPDATE`, `DELETE`** and **`RETURNING`**
- **Aggregates**: `COUNT`, `GROUP BY`
- **`JOIN`** (inner and left): you'll need it for `jobs` ↔ `job_events`
- **NULL behavior**: `IS NULL`, `= NULL` never works

### Left out
Subqueries in depth, `UNION`, `HAVING` tricks, string/date function catalogs.

### Depth ceiling
**Intermediate.** You can write these without help; you don't need to know edge cases.

### Micro-tasks
- [ ] **S-A1 (30m)** Connect with `docker exec -it <container> psql -U <user> <db>`. Create a scratch database. Practice `\dt`, `\d`, `\x`, `\timing`.
- [ ] **S-A2 (1h)** [pgexercises.com](https://pgexercises.com): **Basic** section only. Type every answer; don't peek.
- [ ] **S-A3 (45m)** Hand-write a `jobs` table (`id`, `type`, `payload jsonb`, `status`, `created_at timestamptz default now()`). Insert 5 rows with different statuses.
- [ ] **S-A4 (30m)** Practice `UPDATE ... RETURNING` and `DELETE ... RETURNING`. See what rows come back.
- [ ] **S-A5 (1h)** pgexercises **Joins** and **Aggregates** sections (first ~6 questions each is enough).
- [ ] **S-A6 ⭐ (15m)** Prove `WHERE x = NULL` returns nothing and `IS NULL` works.

### Deliberate failure
1. Inside `BEGIN; ... ROLLBACK;` run `UPDATE jobs SET status='FAILED';` with **no WHERE**. See how many rows you'd have destroyed. Roll back.
2. Run `SELECT count(*) FROM jobs WHERE status = NULL;` and understand why it's 0.

### Closed-book check
- Write: select all `QUEUED` jobs, newest first, limit 10.
- Write: `UPDATE` one job to `RUNNING` and return the updated row.
- Write: count jobs per status.
- What's the difference between `INNER JOIN` and `LEFT JOIN`?

### Exit criteria
- [ ] pgexercises Basic section done, typed by hand.
- [ ] You can write all four closed-book queries on paper.
- [ ] `sql/scratch/a-basics.sql` exists with your own practice queries.
- [ ] You never used an ORM for a query this block.

---

# SQL-B — Schema, Constraints & Transactions
**Use with Block B · Time box: ~3 hours**

### Goal
Express TaskForge's rules in the database itself, so illegal data can't exist even if your Java has a bug.

### Covers
- **DDL**: `CREATE TABLE`, `ALTER TABLE ADD COLUMN`, `DROP`
- **Constraints**: `PRIMARY KEY`, `NOT NULL`, `DEFAULT`, `CHECK (status IN (...))`, `UNIQUE`, `FOREIGN KEY`
- **Indexes (basic)**: `CREATE INDEX` on `(status, created_at)`
- **Transactions**: `BEGIN`, `COMMIT`, `ROLLBACK`; atomic multi-statement writes
- **Optimistic locking in raw SQL**: `UPDATE ... SET version = version + 1 WHERE id = ? AND version = ?` and checking rows affected
- **Reading the error messages** constraints produce

### Left out
`ALTER TYPE`/Postgres enums (use `text` + `CHECK`; enums are painful to migrate), table inheritance, deferrable constraints, composite types.

### Depth ceiling
**Production** on constraints and transactions. **Intermediate** on indexes (deepened in SQL-F).

### Micro-tasks
- [ ] **S-B1 (45m)** Write `jobs` and `job_events` DDL **from scratch** in `sql/scratch/b-schema.sql` (id, status with CHECK, `version`, timestamps, FK from events to jobs). *Then* compare with your Flyway migration, and fix the one that's worse.
- [ ] **S-B2 (30m)** Violate every constraint on purpose: bad status, null in a NOT NULL column, duplicate unique, orphan FK. Read each error message.
- [ ] **S-B3 (45m)** In one transaction: `UPDATE jobs SET status='RUNNING'` **and** `INSERT INTO job_events`. Then force an error in the second statement and verify the first was rolled back.
- [ ] **S-B4 (45m)** Optimistic lock by hand: two `psql` sessions read the same row (version 1). Session 1 updates with `WHERE version=1`; session 2 tries the same, gets **0 rows updated**.
- [ ] **S-B5 ⭐ (15m)** `ALTER TABLE jobs ADD COLUMN ...` on a table with data. Notice what happens to existing rows.

### Deliberate failure
1. Do S-B3 without `BEGIN`. Show a half-written state (job `RUNNING`, no event).
2. Do S-B4 and observe "silent lost update" when you **omit** `AND version = ?`.

### Closed-book check
- Write the `jobs` table with a `CHECK` on status.
- Why is a `CHECK` constraint a second line of defense even with a Java state machine?
- Write the optimistic-lock `UPDATE` and say how you detect failure.
- What does `ROLLBACK` actually undo?

### Exit criteria
- [ ] You wrote `jobs`/`job_events` DDL from memory before looking at your migration.
- [ ] Every constraint was deliberately violated at least once.
- [ ] Two-session optimistic lock demo reproduced and noted in `NOTES.md`.
- [ ] Rolled-back transaction demonstrated with two statements.

---

# SQL-C — Locking & Claiming (the core)
**Use with Block C · Time box: ~5 hours**

> The most important SQL block. The whole queue depends on one statement.

### Goal
Write the claim query yourself, understand every clause, and watch it behave correctly under concurrent sessions.

### Covers
- **Row locks**: `SELECT ... FOR UPDATE` (blocks), what the lock protects, when it releases (at `COMMIT`/`ROLLBACK`)
- **`SKIP LOCKED`**: skip rows locked by others instead of waiting
- **CTE + `UPDATE ... FROM`**: select a row and update it in a single atomic statement
- **`RETURNING`** to hand the claimed job back to the worker
- **Lease math**: `now() + interval '30 seconds'`, comparing against `now()`
- **`now()` vs `clock_timestamp()`**: `now()` is the *transaction start* time. Know why that matters for leases.
- **Conditional UPDATE for fencing**: `WHERE id = ? AND locked_by = ?`, then check rows affected
- **Reaper shape**: find `RUNNING` jobs with expired leases and requeue them
- **Isolation levels in practice**: why default `READ COMMITTED` is fine here

### Left out
Advisory locks, `FOR NO KEY UPDATE`/`FOR SHARE` variants, `NOWAIT` (note it exists), serializable isolation, deadlock analysis beyond "it can happen".

### Depth ceiling
**Production.** You write the claim from a blank file and explain each clause.

### Micro-tasks
- [ ] **S-C1 (45m)** Two psql sessions, `BEGIN` in both. Session 1: `SELECT ... FOR UPDATE` on one row. Session 2: same row. Watch it **block**. Commit session 1 and watch session 2 proceed.
- [ ] **S-C2 (30m)** Repeat with `SKIP LOCKED`. Session 2 should *return a different row* (or none) instead of waiting.
- [ ] **S-C3 (1h)** Write the naive claim as two statements (`SELECT` then `UPDATE`). Run it in two sessions interleaved by hand and cause a double-claim.
- [ ] **S-C4 (1.5h)** Write the **single-statement claim** yourself: a CTE that picks one eligible `QUEUED` job (ordered, limited to 1, `FOR UPDATE SKIP LOCKED`), then updates it to `RUNNING` with `locked_by` and `lease_expires_at`, returning the row. Test it with 3 sessions and 10 rows. No row claimed twice.
- [ ] **S-C5 (30m)** Write the **heartbeat** (`UPDATE ... SET lease_expires_at = ... WHERE id=? AND locked_by=?`) and the **completion** with fencing (same `WHERE locked_by`). Verify "0 rows updated" when the owner has changed.
- [ ] **S-C6 (30m)** Write the **reaper**: requeue `RUNNING` jobs whose lease has expired. Test by setting a lease in the past.
- [ ] **S-C7 ⭐ (15m)** Run `SELECT now(); SELECT pg_sleep(3); SELECT now();` inside one `BEGIN` and compare to `clock_timestamp()`.

**Hint on shape, not answer:** your claim has three parts: *(1) pick*, *(2) lock-and-skip*, *(3) update-and-return*. Write each in isolation first, then combine.

### Deliberate failure
1. S-C3: demonstrate the double-claim with the naive two-step approach.
2. Remove `SKIP LOCKED` from your final claim; observe two workers serialize/block.
3. Remove `FOR UPDATE` but keep the CTE; try to reproduce the race with 3 sessions. Note what you observe.
4. Complete a job with the **wrong** `locked_by`; confirm 0 rows updated.

### Closed-book check
- Write the claim query on paper, no notes.
- Explain each clause: `ORDER BY`, `LIMIT 1`, `FOR UPDATE`, `SKIP LOCKED`, `RETURNING`.
- Why `now()` from the DB and not `Instant.now()` from the worker?
- What does `UPDATE ... WHERE locked_by = ?` returning 0 rows tell the worker?
- When is the row lock released?

### Exit criteria
- [ ] Claim query written and explained from a blank file.
- [ ] Reproduced the naive double-claim in two psql sessions.
- [ ] Heartbeat, fenced completion and reaper queries exist in `sql/queries/` and are used by TaskForge via native SQL.
- [ ] Wrong-owner completion returns 0 rows, proven in psql.
- [ ] You can explain `now()` vs `clock_timestamp()`.

---

# SQL-D — Time, Retries & Moving Rows
**Use with Block D · Time box: ~2.5 hours**

### Goal
Express retry scheduling and dead-lettering in SQL.

### Covers
- **Intervals and timestamp arithmetic**: `now() + interval '10 seconds'`, `make_interval(secs => ...)`
- **`next_run_at <= now()`** as the eligibility filter
- **Math in SQL**: `power(2, attempts)`, `least()`, `random()`. Decide and document: compute backoff in **Java** (testable) or **SQL**. (Recommendation: Java, pass the result in.)
- **Incrementing counters atomically**: `SET attempts = attempts + 1`
- **Moving rows**: `INSERT INTO dead_jobs SELECT ...`
- **`DELETE ... RETURNING` inside a CTE** to move rows in one atomic statement
- **`COALESCE`** for nullable fields like `last_error`

### Left out
Date functions catalog, time zones in depth (store `timestamptz`, think UTC), recursive retries.

### Depth ceiling
**Intermediate.**

### Micro-tasks
- [ ] **S-D1 (30m)** Write the **retry update**: set status `RETRYING`, increment `attempts`, set `next_run_at = now() + <delay>`, store `last_error`.
- [ ] **S-D2 (30m)** Modify your claim to only pick rows where `next_run_at <= now()`. Test with one future and one past row.
- [ ] **S-D3 (45m)** Write the **dead-letter move** as a single statement: delete from `jobs` (or mark) and insert into `dead_jobs` atomically using a CTE with `DELETE ... RETURNING`.
- [ ] **S-D4 (30m)** Write `SELECT` that shows backoff delays per attempt with `power()`, then re-implement in Java and compare outputs.
- [ ] **S-D5 ⭐ (15m)** Add jitter in SQL with `random()`; decide which side owns it and write one line in `tradeoffs.md`.

### Deliberate failure
1. Do the dead-letter move as two separate statements, kill the session between them, and show a job vanishing or duplicating.
2. Use `now()` in Java and in SQL for `next_run_at` in different places; note any mismatch.

### Closed-book check
- Write the retry update on paper.
- Why is moving a row to `dead_jobs` safer as one statement than two?
- Why `timestamptz` instead of `timestamp`?
- Where does your backoff computation live and why?

### Exit criteria
- [ ] Retry update and dead-letter move both in `sql/queries/`.
- [ ] Dead-letter move proven atomic (kill-between-statements demo contrasted).
- [ ] Claim respects `next_run_at`, proven with test rows.

---

# SQL-E — Upserts & Idempotency
**Use with Block E · Time box: ~2 hours**

### Goal
Replace "check then insert" with a single atomic statement.

### Covers
- **`UNIQUE` constraints as the real guard**
- **`INSERT ... ON CONFLICT DO NOTHING`**
- **`INSERT ... ON CONFLICT (cols) DO UPDATE`** (upsert)
- **The `RETURNING` gotcha**: with `DO NOTHING`, a conflicting insert returns **no row**, so you need a follow-up `SELECT` or a no-op `DO UPDATE`
- **Why check-then-insert is a race** under concurrency
- **Composite unique keys**: `(type, idempotency_key)`

### Left out
`MERGE` (newer syntax), partial unique indexes with conflict targets (aware only), exclusion constraints.

### Depth ceiling
**Production.** This pattern shows up everywhere.

### Micro-tasks
- [ ] **S-E1 (30m)** Add `UNIQUE (type, idempotency_key)`. Insert a duplicate and read the error.
- [ ] **S-E2 (30m)** Write `INSERT ... ON CONFLICT DO NOTHING`. Observe 0 rows inserted on conflict.
- [ ] **S-E3 (30m)** Solve the returning gotcha: make the statement return the **existing** job's ID whether it inserted or conflicted. Try two approaches and note tradeoffs.
- [ ] **S-E4 (30m)** Show check-then-insert failing: two psql sessions, both `SELECT` (nothing), both `INSERT`; one hits the unique error or, without the constraint, you get duplicates.

### Deliberate failure
1. Remove the `UNIQUE` constraint and let two sessions both insert. Count duplicates.
2. Use `DO NOTHING` and try to read the ID from `RETURNING`; see it come back empty.

### Closed-book check
- Write `INSERT ... ON CONFLICT DO NOTHING` for the jobs table.
- Why does `SELECT` then `INSERT` fail under concurrency even in a transaction at `READ COMMITTED`?
- Why must a `UNIQUE` constraint exist for `ON CONFLICT` to work?
- How do you get the existing row's ID after a conflict?

### Exit criteria
- [ ] Idempotent insert query in `sql/queries/` and used by `POST /jobs`.
- [ ] Check-then-insert race demonstrated by hand.
- [ ] RETURNING gotcha understood and solved.

---

# SQL-F — Plans, Indexes & Performance
**Use with Block F · Time box: ~3 hours**

### Goal
Prove your claim query is fast at 100k rows and know why.

### Covers
- **`generate_series`** to seed 100k rows quickly
- **`EXPLAIN` vs `EXPLAIN ANALYZE`**, and reading: node type, cost, actual time, rows, loops
- **Seq Scan vs Index Scan vs Index Only Scan**
- **Composite index column order** `(status, priority, created_at)`
- **Partial indexes**: `CREATE INDEX ... WHERE status = 'QUEUED'`
- **Why the planner sometimes ignores your index** (low selectivity, stale stats; `ANALYZE`)
- **Cancel as a conditional update**: `UPDATE ... WHERE id=? AND status='QUEUED'`, rows affected tells you who won

### Left out
Index types beyond B-tree (GIN, GiST, BRIN), planner cost tuning, `pg_stat_statements` setup, query hints, `VACUUM`/autovacuum tuning.

### Depth ceiling
**Intermediate.** You read a plan and make one correct index. You don't tune the planner.

### Micro-tasks
- [ ] **S-F1 (30m)** Seed 100k jobs with `generate_series` (95% `SUCCEEDED`, 5% `QUEUED`).
- [ ] **S-F2 (45m)** Run `EXPLAIN ANALYZE` on your claim query **with no helpful index**. Record: plan type and time.
- [ ] **S-F3 (45m)** Add a **partial index** on queued rows matching your claim's `ORDER BY`. Re-run, record the difference.
- [ ] **S-F4 (30m)** Try a regular full index too and compare size (`\di+`) and plan to the partial one.
- [ ] **S-F5 (30m)** Cancel race: two sessions, one claims, one cancels the same `QUEUED` row. Only one `UPDATE` returns a row.
- [ ] **S-F6 ⭐ (15m)** Run `ANALYZE jobs;` and see if the plan changes.

### Deliberate failure
1. Run the claim at 100k rows with no index. Note the Seq Scan.
2. Change the claim's `ORDER BY` so it no longer matches the index; see the index stop helping.

### Closed-book check
- What's the difference between `EXPLAIN` and `EXPLAIN ANALYZE`?
- What does "Seq Scan" on a 100k-row queue table tell you?
- Why is a partial index smaller and faster for a queue?
- How does a conditional `UPDATE` decide a cancel-vs-claim race?

### Exit criteria
- [ ] Before/after `EXPLAIN ANALYZE` timings for the claim recorded in `NOTES.md`.
- [ ] Partial index created via a Flyway migration.
- [ ] Cancel-vs-claim race resolved with a conditional update and demonstrated.

---

# SQL-G — Metrics & Introspection Queries
**Use with Block G · Time box: ~1.5 hours**

### Goal
Ask the database how the queue is doing, and see what your workers are doing to it.

### Covers
- **Queue depth**: `SELECT status, count(*) ... GROUP BY status`
- **Wait time**: `started_at - created_at`
- **Percentiles**: `percentile_cont(0.95) WITHIN GROUP (ORDER BY ...)`
- **`pg_stat_activity`**: who's connected, what they're running, who is `idle in transaction`
- **`pg_locks` (glance)**: see who's blocking whom

### Left out
`pg_stat_statements`, window functions for rolling metrics, Prometheus SQL exporters.

### Depth ceiling
**Basic→Intermediate.** Useful for debugging and the README numbers.

### Micro-tasks
- [ ] **S-G1 (30m)** Write queue-depth-by-status and oldest-queued-job-age queries.
- [ ] **S-G2 (30m)** Write p50/p95/p99 of wait time and run time using `percentile_cont`. Paste the numbers into your README.
- [ ] **S-G3 (30m)** While 4 workers run, watch `pg_stat_activity`. Then shrink the pool to 2 and spot connections stuck `idle in transaction` or waiting.

### Deliberate failure
Hold a transaction open in one psql session and watch it appear in `pg_stat_activity` as `idle in transaction`. That's what a leaked connection looks like.

### Closed-book check
- Why does p99 wait time matter more than the average?
- What does `idle in transaction` mean and why is it dangerous for a queue?

### Exit criteria
- [ ] Metrics queries saved in `sql/queries/metrics.sql`.
- [ ] README numbers sourced from your own `percentile_cont` output.
- [ ] You can spot a leaked/idle connection in `pg_stat_activity`.

---

## 2. Final SQL gate (end of Phase 1)

On paper, no notes, no AI. Each must be correct:

- [ ] `jobs` table with `CHECK`, `UNIQUE (type, idempotency_key)`, and a partial index.
- [ ] The **claim query**, with each clause explained.
- [ ] The **heartbeat**, **fenced completion** and **reaper** queries.
- [ ] The **dead-letter move** as one atomic statement.
- [ ] The **idempotent insert** with `ON CONFLICT`, returning the existing ID.
- [ ] Explain a `Seq Scan` vs `Index Scan` from a plan you ran.
- [ ] Explain `now()` vs `clock_timestamp()`.

If you can do these, you've moved from "I paste what an LLM gives me" to "I own this layer."

---

## 3. How it fits the main plan

| Week | TaskForge blocks | SQL blocks |
|---|---|---|
| 1 | A + B | SQL-A, SQL-B |
| 2 | C (first half) | SQL-C |
| 3 | C (second half) + D | SQL-C finish, SQL-D |
| 4 | D finish + E | SQL-E |
| 5 | F + G | SQL-F, SQL-G |

If time is short, cut ⭐ tasks first. Never cut SQL-C or the closed-book checks.

---

## 4. Daily rhythm

1. **Start (5 min):** open `sql/NOTES.md`, read yesterday's last entry.
2. **Work:** one S-task, typed in psql first.
3. **End (5 min):** add the query to `sql/queries/` (if final) and log what surprised you.

> If you can't write it in psql, you can't write it in Java.
