# PHASE 1 — Asynchrony, Jobs, and State Machines
### Deliverable: **TaskForge** — a Postgres-backed job execution engine

> **One sentence:** Build one excellent job engine, because a build, a deploy, and a chunk replication are all just *jobs*.
> **Budget:** ~4–5 weeks · ~55–60 hours at 10–15 hrs/week
> **You arrive with (from Phase 0):** transactions, isolation levels, row locks, Testcontainers, Flyway, k6. Reuse all of it. Do not re-learn it.

---

## 0. How to use this file

1. Work **one block at a time, in order**. Never open Block N+1 until Block N's exit criteria are all ticked.
2. Every block has: **Goal → Covers → Left out → Depth ceiling → Micro-tasks → Deliberate failure → Closed-book check → Exit criteria → Time box**.
3. A micro-task is **1–3 hours**. If one is bigger, split it before starting.
4. **Build the failure.** Each block has a failure drill. A block is not done until you have *seen* the bug the block exists to prevent.
5. Closed-book check = answer on paper with no notes, no AI. If you can't, you don't own it yet.

### Anti-loop rules (read these when you feel stuck)

| Situation | Rule |
|---|---|
| Stuck on one error > **90 minutes** | Stop. Write down: *what I expected, what happened, what I tried.* Then take a 10-min break. |
| Still stuck after **one full day** | Ask for a hint (not the solution). The writeup above is your question. |
| Tempted to read about a "better" tool | Write it in `parking-lot.md`. Do not open the link until Phase 1 is done. |
| Block is overrunning its time box by **50%** | Cut scope to the *minimum exit criteria*. Ship rough, move on. Note it in `tradeoffs.md`. |
| You want to polish | Exit criteria met? Then move on. Polish is not an exit criterion. |
| Unsure what to build next | Build the **failure case**, not a feature. |

### Scope freeze

TaskForge is **only** these: submit job → claim job → run handler → record outcome → survive failures. No UI. No auth. No multi-tenancy. No cron expressions. Anything else goes in `parking-lot.md`.

---

## 1. The map

```
BLOCK A  Async foundations         (threads, executors, 202 pattern)         ~6h
   ↓
BLOCK B  Job model & state machine (persist, legal transitions)              ~7h
   ↓
BLOCK C  Safe claiming             (leases, heartbeats, SKIP LOCKED)         ~12h   ← the core
   ↓
BLOCK D  Failure handling          (retries, backoff+jitter, DLQ, shutdown)  ~10h
   ↓
BLOCK E  Idempotency & delivery    (idempotency keys, at-least-once)         ~6h
   ↓
BLOCK F  Scheduling & control      (priority, delay, cancellation)           ~8h
   ↓
BLOCK G  Observe, prove, ship      (metrics, chaos doc, README)              ~6h
```

**Why this order:** each block creates the problem the next block solves.
A makes jobs run in the background → B makes them survive restart → C makes many workers safe → D makes failure normal → E makes retries harmless → F adds control → G proves it.

### Mapping to the roadmap's 8 build steps

| Roadmap step | Where it lives |
|---|---|
| 1. In-memory queue, one handler | Block A |
| 2. Persist to Postgres, see stuck `RUNNING` | Block B |
| 3. Leasing + heartbeats | Block C |
| 4. Multi-worker `SKIP LOCKED`, exactly-once test | Block C |
| 5. Retries, backoff + jitter, DLQ | Block D |
| 6. Idempotency keys | Block E |
| 7. Priorities, delay, cancellation | Block F |
| 8. Metrics | Block G |

---

## 2. Repo skeleton (set up in Block A, task A0)

```
taskforge/
├── README.md              # problem → design decisions → tradeoffs (written last, drafted continuously)
├── docs/
│   ├── state-machine.md   # the diagram + every illegal transition
│   ├── tradeoffs.md       # decisions + "what I'd do differently"
│   ├── chaos-log.md       # every failure you injected and what happened
│   ├── error-log.md       # error → cause → fix (interview material)
│   └── parking-lot.md     # ideas you're NOT doing now
├── src/main/...
├── src/main/resources/db/migration/   # Flyway only. No ddl-auto.
└── src/test/...           # Testcontainers for every DB-touching test
```

Keep `error-log.md` from day one. Every bug gets three lines: *symptom, root cause, fix.*

---

## Test inventory (your proof)

These are the tests that matter more than any coverage number:

| Test | Proves | Block |
|---|---|---|
| Illegal transition rejected (parameterized) | State machine enforcement | B |
| Two writers, one row → optimistic lock exception | `@Version` works | B |
| 4 workers × 1000 jobs → 1000 unique side effects | No double execution | C |
| Heartbeat stops → lease expires → job reclaimed | Failure recovery | C |
| Zombie worker's late result rejected | Fencing | C |
| Backoff is monotonic, capped, jittered within bounds | Retry policy | D |
| Exhausted attempts → `DEAD` + row in `dead_jobs` | DLQ | D |
| SIGTERM mid-job → no job left `RUNNING` | Graceful shutdown | D |
| 20 concurrent identical submits → 1 job | Idempotent submission | E |
| Crash after effect, before ack → 1 effect | Idempotent handler | E |
| Cancel vs claim race ×1000 → consistent | Cancellation safety | F |

---


# BLOCK A — Async Foundations
**Time box: ~6 hours · Roadmap step 1**

### Goal
A request returns immediately with an ID while work happens in the background. In-memory only. It will lose everything on restart, and that's the point.

### Covers
- **Thread pools & `ExecutorService`**: core/max size, bounded queue, rejection policy
- **Spring `@Async` / `TaskExecutor`**: why the default `SimpleAsyncTaskExecutor` is a trap (creates a thread per task, no cap)
- **The 202 Accepted pattern**: `POST /jobs` → `202` + job ID + `Location` header; `GET /jobs/{id}` to poll
- **Handler abstraction**: a `JobHandler` interface keyed by job type (design this now, you'll reuse it all phase)

### Left out (intentionally)
| Skipped | Why |
|---|---|
| `CompletableFuture` chaining, reactive, `Flux`/`Mono` | Not needed; jobs are fire-and-track |
| Virtual threads deep dive | Aware they exist; revisit after Phase 1 |
| `ForkJoinPool`, parallel streams | Wrong tool for I/O-ish jobs |
| Spring Batch, Quartz, JobRunr | That's *the thing you're building*. Don't import the answer. |
| Kafka / RabbitMQ / Redis | Deliberate: feel Postgres queue limits first |

### Depth ceiling
**Intermediate.** You must be able to explain *why* an unbounded pool is a memory leak and what a rejection policy does. You do **not** need to know ThreadPoolExecutor's internals line by line.

### Micro-tasks
- [x] **A0 (1h)** Create repo skeleton, Spring Boot + Postgres via Docker Compose, Flyway wired (empty migration), one Testcontainers smoke test passes.
- [x] **A1 (1h)** Define `JobHandler` interface + one handler (`EchoJobHandler` that sleeps N seconds then returns).
- [ ] **A2 (1.5h)** Hand-build a `ThreadPoolExecutor` (bounded queue, named threads, explicit rejection policy). No `@Async` yet.
- [ ] **A3 (1h)** `POST /jobs` → `202 Accepted` + ID. `GET /jobs/{id}` returns status from an in-memory `ConcurrentHashMap`.
- [ ] **A4 (1h)** Now swap to Spring `TaskExecutor` bean config. Compare to A2: what did Spring save you?
- [ ] **A5 (0.5h)** Write 3 lines in `docs/tradeoffs.md`: why `202` and not blocking `200`.

### Deliberate failure
1. Submit 10,000 jobs against an **unbounded** queue. Watch heap grow (`jconsole` or `jcmd`).
2. Switch to a bounded queue; submit again. Observe what happens when it's full. Make the API return `429`/`503` instead of crashing.
3. Restart the app mid-job. Everything's gone. *Screenshot it. This motivates Block B.*

### Closed-book check
- Why does `Executors.newCachedThreadPool()` risk killing the JVM?
- What are the 4 built-in rejection policies and which would you pick for a job API?
- Why is `202` the right response and what must it include?

### Exit criteria
- [ ] Submit a job, get `202` in < 50ms, poll until `SUCCEEDED`.
- [ ] Queue full → client gets a clean error, app survives.
- [ ] You can explain the unbounded-pool memory leak from your own heap observation.
- [ ] Restart loses jobs, and you've written down *why* that's unacceptable.

---

# BLOCK B — Job Model & State Machine
**Time box: ~7 hours · Roadmap step 2**

### Goal
Jobs live in Postgres. Status changes follow **explicit, enforced rules**. Restart mid-job and observe the job stuck in `RUNNING` forever. That bug is your motivation for Block C.

### Covers
- **Schema design** for `jobs`: `id`, `type`, `payload (jsonb)`, `status`, `attempts`, `max_attempts`, `created_at`, `updated_at`, `started_at`, `finished_at`, `last_error`, `version`
- **State machine**: `QUEUED → RUNNING → SUCCEEDED | FAILED | CANCELLED` (+ `RETRYING` later in D)
- **Enforcing transitions in code**: one place decides legality; throw on illegal transitions
- **Transition history table** (`job_events`): every transition persisted and timestamped
- **Optimistic locking (`@Version`)**: first exposure, one job row and two writers

### Left out (intentionally)
| Skipped | Why |
|---|---|
| State machine libraries (Spring Statemachine) | Hides the learning. A `Map<Status, Set<Status>>` is enough. |
| Event sourcing / CQRS | Overkill; `job_events` is a simple audit log |
| Fancy JSON querying on payload | Store it, don't query into it |
| Partitioning/archiving old jobs | Scale concern; none yet |

### Depth ceiling
**Production** on state machines (you will enforce them everywhere later). **Intermediate** on optimistic locking.

### Micro-tasks
- [ ] **B1 (1h)** Draw the state machine on paper: every state, every legal arrow, then list every **illegal** arrow (e.g. `SUCCEEDED → RUNNING`). Save to `docs/state-machine.md`.
- [ ] **B2 (1h)** Flyway migration: `jobs` + `job_events` tables, with a CHECK constraint on `status`, and an index on `(status, created_at)`.
- [ ] **B3 (1.5h)** `JobStatus` enum + `JobTransitions` class (single source of truth for legality). Unit-test every legal and every illegal transition.
- [ ] **B4 (1.5h)** `JobRepository` + `JobService.transition(jobId, from, to)` that writes the job row **and** the `job_events` row in one transaction.
- [ ] **B5 (1h)** Replace the in-memory map from Block A with Postgres. Executor now pulls from DB.
- [ ] **B6 (1h)** Add `@Version`. Write a test: two threads load the same job, both try to update, one gets `OptimisticLockException`.

### Deliberate failure
1. Start a long job (60s). `kill -9` the app mid-run. Restart.
2. Query: the job is `RUNNING` forever. Nobody owns it, nobody will finish it. **Write this in `error-log.md`.**
3. Try to force an illegal transition via a direct service call; confirm it's rejected.

### Closed-book check
- Name three illegal transitions and say what breaks if each were allowed.
- Why does the transition write and the event write share one transaction?
- What does `@Version` do on `UPDATE`, in SQL terms?

### Exit criteria
- [ ] Every transition is enforced in code and covered by a test.
- [ ] `job_events` gives a full, timestamped history of any job.
- [ ] Stuck-`RUNNING` reproduced on purpose and documented.
- [ ] Optimistic-lock collision demonstrated in a test.
- [ ] State machine doc includes the illegal transitions.

---

# BLOCK C — Safe Claiming (the core of Phase 1)
**Time box: ~12 hours · Roadmap steps 3 and 4**

> This is the hardest and most valuable block. If you must cut time elsewhere, never here.

### Goal
Many workers, one database, **every job executes exactly once**, and a dead worker's jobs become claimable again.

### Covers
- **`SELECT … FOR UPDATE SKIP LOCKED`**: how a Postgres table becomes a real queue
- **Atomic claim**: one statement that picks a job and marks it `RUNNING` with an owner
- **Leases**: `locked_by`, `lease_expires_at`; ownership is time-limited
- **Heartbeats**: running workers extend their lease periodically
- **Lease reaper**: finds expired `RUNNING` jobs and makes them claimable again
- **Fencing idea**: a worker that lost its lease must not overwrite the new owner's result (checked via `locked_by`/`version` on completion)
- **Worker loop**: poll interval, empty-queue backoff, batch size 1

### Left out (intentionally)
| Skipped | Why |
|---|---|
| `LISTEN/NOTIFY` for push wakeups | Optimization; plain polling is fine |
| Advisory locks | `SKIP LOCKED` covers your need |
| Distributed locks (Redis, ZooKeeper, ShedLock) | Not needed; the DB *is* your coordinator |
| Claiming batches of N jobs | Adds complexity; claim one at a time |
| Leader election | Reaper can run on every worker safely (idempotent) |
| Clock-skew theory | Use the DB's `now()` for leases, never the app's clock. That's all you need. |

### Depth ceiling
**Production.** You must be able to write the claim query from memory and explain every clause.

### Micro-tasks
- [ ] **C1 (1.5h)** Reproduce the double-execution bug: 2 workers, naive `SELECT` then `UPDATE`. Show the same job run twice. Log it.
- [ ] **C2 (2h)** Fix with `FOR UPDATE SKIP LOCKED` claim in a single transaction. Run 2 workers manually, confirm no duplicates.
- [ ] **C3 (1.5h)** Add `locked_by`, `lease_expires_at` columns (new Flyway migration). Claim sets both using DB time.
- [ ] **C4 (1.5h)** Heartbeat: a scheduled task extends `lease_expires_at` for jobs the worker currently holds. Test: heartbeat stops → lease expires.
- [ ] **C5 (1.5h)** Lease reaper: expired `RUNNING` → back to `QUEUED` (increment `attempts`). Revisit your Block B stuck-job bug. It now self-heals.
- [ ] **C6 (1.5h)** Fencing: completion `UPDATE ... WHERE id=? AND locked_by=?` — if 0 rows updated, the worker lost ownership and must discard its result. Test it with a deliberately slow worker.
- [ ] **C7 (2h)** **The big test**: 4 worker processes (or threads with separate DataSources), 1000 jobs, each job inserts a row into a `side_effects` table. Assert exactly 1000 rows, each `job_id` unique, zero missing.
- [ ] **C8 (0.5h)** Add a unique constraint on `side_effects.job_id` so a duplicate fails *loudly* rather than being counted.

### Deliberate failure
1. Run C1 naively and capture the duplicates.
2. `kill -9` a worker mid-job. Time how long until another worker finishes it (≈ lease duration).
3. Make a worker pause (`kill -STOP`) past its lease, resume it (`kill -CONT`), and watch fencing reject its late result.
4. Set lease shorter than job duration and **without** heartbeat, observe jobs getting stolen mid-run. Then turn the heartbeat on.

### Closed-book check
- Write the claim query from memory. Explain `SKIP LOCKED` vs plain `FOR UPDATE` (blocking vs skipping).
- Why use the **database's** clock for lease expiry?
- What problem does fencing solve that the lease alone does not?
- What's the tradeoff of a short vs a long lease?

### Exit criteria
- [ ] 4 workers × 1000 jobs → exactly 1000 side effects, proven by an automated test, not by reading logs.
- [ ] `kill -9` a worker mid-job → another worker completes it.
- [ ] Paused-then-resumed zombie worker cannot overwrite the new owner's result.
- [ ] You can write the claim SQL from a blank file.
- [ ] Double-execution bug (C1) is documented in `error-log.md`.

---

# BLOCK D — Failure Handling
**Time box: ~10 hours · Roadmap step 5 + graceful shutdown**

### Goal
Failure is a normal path, not an exception. Handlers fail, workers get `SIGTERM`, and the system handles it with bounded, de-synchronized retries.

### Covers
- **Retry policy**: `max_attempts`, per-job-type overrides
- **Exponential backoff**: `delay = base × 2^attempt`, capped
- **Jitter**: why synchronized retries cause a thundering herd; "full jitter" formula
- **`next_run_at`**: retry scheduling is just "claim only if `next_run_at <= now()`"
- **Error classification**: *retryable* (timeout) vs *permanent* (bad payload)
- **Dead-letter table**: jobs that exhaust attempts land in `dead_jobs`, with final error, kept for inspection and manual requeue
- **Graceful shutdown**: on `SIGTERM`, stop claiming, let in-flight finish within a deadline, else release the lease
- **`RETRYING` / `DEAD` states** added to the state machine (update your doc and tests)

### Left out (intentionally)
| Skipped | Why |
|---|---|
| Circuit breakers (Resilience4j) | Phase 4, between services |
| Retry budgets / adaptive throttling | Scale concern |
| Per-error retry rules DSL | A boolean `retryable` is enough |
| Automatic DLQ reprocessing | Manual requeue endpoint only |
| Poison-pill detection heuristics | `max_attempts` already bounds it |

### Depth ceiling
**Production** on backoff + jitter + DLQ. **Intermediate** on graceful shutdown.

### Micro-tasks
- [ ] **D1 (1h)** Extend state machine: add `RETRYING` and `DEAD`. Update `state-machine.md` and transition tests.
- [ ] **D2 (1.5h)** Migration: `attempts`, `max_attempts`, `next_run_at`, `last_error`. Claim query now filters `next_run_at <= now()`.
- [ ] **D3 (1.5h)** Implement `BackoffPolicy` (pure function, no Spring). Unit-test it: monotonic growth, cap respected, jitter within bounds.
- [ ] **D4 (1.5h)** Handler throws → job goes `RETRYING` with computed `next_run_at`. After `max_attempts` → `DEAD` and row copied to `dead_jobs`.
- [ ] **D5 (1h)** `RetryableException` vs `PermanentException`; permanent errors skip retries.
- [ ] **D6 (2h)** Graceful shutdown: on `SIGTERM`, stop polling, wait up to N seconds for in-flight jobs, then release unfinished leases back to `QUEUED`.
- [ ] **D7 (1h)** `POST /dead-jobs/{id}/requeue` endpoint. Resets attempts, moves back to `QUEUED`.
- [ ] **D8 (0.5h)** Add a chart/print of retry delays across 8 attempts, with and without jitter.

### Deliberate failure
1. Create a handler that fails 100% of the time, with **no jitter**, 500 jobs. Plot retry timestamps: they cluster in spikes. That's the thundering herd.
2. Add jitter. Re-plot. The spikes flatten. *Keep both plots for your write-up.*
3. Send `SIGTERM` mid-job. Verify the job finishes or is cleanly released, never stuck.
4. Send `kill -9` after `SIGTERM` grace period. Lease reaper (Block C) must still catch it.

### Closed-book check
- Write the full-jitter formula. Why is *no* jitter dangerous?
- Difference between `SIGTERM` and `SIGKILL` for your worker's behavior?
- Why does a DLQ exist instead of retrying forever?
- Which exceptions should *never* be retried, and why?

### Exit criteria
- [ ] Failing handler retries with growing, jittered delays and ends in `DEAD`.
- [ ] Permanent errors skip retries.
- [ ] `SIGTERM` mid-job leaves **zero** jobs stuck in `RUNNING`.
- [ ] Before/after jitter plot exists.
- [ ] State machine doc and tests updated for new states.

---

# BLOCK E — Idempotency & Delivery Semantics
**Time box: ~6 hours · Roadmap step 6**

### Goal
Submitting the same job twice creates one job, and a job that runs twice (because retries and lease expiry *guarantee* it eventually will) causes one effect.

### Covers
- **Idempotency keys on submission**: client supplies `Idempotency-Key`; unique constraint `(type, key)`; second submit returns the original job
- **Handler-side idempotency**: making the *effect* safe to repeat (unique constraint, upsert, "check-then-act inside one transaction")
- **At-most-once vs at-least-once vs exactly-once**: why exactly-once *delivery* is impossible; why at-least-once + idempotent handler = effectively-once
- **Same key, different payload**: reject with `409` (a client bug, not a duplicate)
- **Where your system is at-least-once** (lease expiry, retries) and where the effect boundary lives

### Left out (intentionally)
| Skipped | Why |
|---|---|
| Kafka exactly-once semantics / transactions | Phase 4, and it's a nuance on top of this |
| Two-phase commit, sagas | Phase 4 |
| Idempotency key expiry/TTL policies | Keep keys forever for now; note it as a tradeoff |
| Request-hash fingerprinting beyond simple equality | Compare payload JSON; good enough |

### Depth ceiling
**Production** on idempotency keys and the at-least-once reasoning. **Intermediate** on the delivery-semantics theory. You need the argument, not the literature.

### Micro-tasks
- [ ] **E1 (1h)** Migration: `idempotency_key` column + `UNIQUE (type, idempotency_key)`.
- [ ] **E2 (1.5h)** `POST /jobs` with `Idempotency-Key`: same key → same job ID, `200` (or `202`) with the original. Use `INSERT ... ON CONFLICT` rather than check-then-insert.
- [ ] **E3 (1h)** Same key + different payload → `409 Conflict`.
- [ ] **E4 (1.5h)** Race test: 20 threads submit the same key simultaneously → exactly one row.
- [ ] **E5 (1h)** Make a handler idempotent by effect (e.g. `INSERT ... ON CONFLICT DO NOTHING` on `side_effects`). Re-run the same job twice deliberately; one effect.

### Deliberate failure
1. Submit the same job 20× concurrently **without** the constraint. Count duplicates.
2. Add the constraint. Re-run. Duplicates gone.
3. Force a handler to complete its side effect and then crash *before* marking the job `SUCCEEDED`. Watch it re-run. Prove your handler survives it.

### Closed-book check
- Explain, in your own words and unprompted, why exactly-once delivery is impossible. (Hint: two-generals / ack-lost problem.)
- Why is check-then-insert unsafe, and what replaces it?
- Where in TaskForge can a job run twice even with perfect code?
- What's the difference between an idempotent *submission* and an idempotent *handler*?

### Exit criteria
- [ ] 20 concurrent identical submissions → exactly 1 job.
- [ ] Crash-after-effect-before-ack scenario produces exactly 1 effect.
- [ ] Same key + different payload → `409`.
- [ ] You can give the "exactly-once is impossible, here's what I did instead" answer out loud, without notes.

---

# BLOCK F — Scheduling & Control
**Time box: ~8 hours · Roadmap step 7**

### Goal
Jobs can be prioritized, delayed, and cancelled. Cancellation works for queued *and* running jobs.

### Covers
- **Priorities**: `priority` int column; claim orders by `priority DESC, created_at ASC`; **starvation** and a simple aging rule
- **Delayed jobs**: `run_at` (reuse `next_run_at` from Block D)
- **Cancellation, two flavors**:
  - `QUEUED` → `CANCELLED` directly (simple, one UPDATE)
  - `RUNNING` → **cooperative**: set `cancel_requested = true`; handler checks a `CancellationToken` and stops
- **Handler timeouts**: max runtime per job; exceeding it counts as a failure
- **Index tuning for the claim query**: `EXPLAIN ANALYZE` your claim with 100k queued jobs and add the right partial index (Phase 0 skills reused)

### Left out (intentionally)
| Skipped | Why |
|---|---|
| Cron / recurring schedules | Separate problem domain; parking lot |
| Forced thread kill (`Thread.stop`) | Unsafe. Cooperative only. |
| Multiple queues / named queues / rate limits per queue | Scale/product feature |
| Job dependencies / DAGs / workflows | That's Temporal/Airflow territory |
| Fair scheduling across tenants | No tenants in TaskForge |

### Depth ceiling
**Intermediate.** One correct implementation of each, tested. No frameworks, no cleverness.

### Micro-tasks
- [ ] **F1 (1h)** Add `priority` to schema and claim ordering. Test: high-priority jobs claimed first.
- [ ] **F2 (1h)** Starvation demo: flood with high-priority jobs, low-priority never runs. Add a simple aging rule *or* document the limitation in `tradeoffs.md`.
- [ ] **F3 (1h)** Delayed jobs: `POST /jobs` with `runAt`. Verify not claimed early.
- [ ] **F4 (1h)** Cancel `QUEUED` job: `POST /jobs/{id}/cancel` → `CANCELLED`. Test a race: cancel vs claim at the same instant, exactly one wins.
- [ ] **F5 (2h)** Cooperative cancel of `RUNNING` job: `cancel_requested` flag + `CancellationToken` checked in a loop handler. Worker finalizes as `CANCELLED`.
- [ ] **F6 (1h)** Per-job timeout. Handler that ignores cancellation: what happens? Document it honestly.
- [ ] **F7 (1h)** `EXPLAIN ANALYZE` the claim at 100k rows; add a partial index `WHERE status='QUEUED'`; record before/after.

### Deliberate failure
1. Write a handler that **never checks** the cancel flag. Cancel it. It keeps running. Why? Document that cooperative means *cooperative*.
2. Cancel a job in the same millisecond a worker claims it. Run 1000 times; state stays consistent every time.
3. Starve a low-priority job. Observe it. Decide.

### Closed-book check
- Why is cooperative cancellation the only safe approach on the JVM?
- Which transitions can a cancel request legally trigger, from which states?
- What index makes the claim query fast, and why a *partial* one?

### Exit criteria
- [ ] Priority ordering test passes.
- [ ] Delayed job never runs early.
- [ ] Cancel works for queued and running jobs, with a race test for the queued case.
- [ ] Claim query on 100k queued rows uses an index scan; before/after timing recorded.
- [ ] Non-cooperative handler behavior is documented, not hidden.

---

# BLOCK G — Observe, Prove, Ship
**Time box: ~6 hours · Roadmap step 8 + closing**

### Goal
The system explains itself, and you've proven the hard properties. This is what turns a project into a portfolio piece.

### Covers
- **Metrics (Micrometer → Prometheus format)**: queue depth, job wait time (created→started), execution time, failure rate, retries, dead count, lease expirations
- **Histograms, not averages**: p50/p95/p99 and why averages lie
- **Structured logs with a `jobId` correlation field** (MDC)
- **Chaos log**: every failure you injected, observed behavior, fix
- **README as an argument**: problem → design decisions → tradeoffs → what I'd do differently
- **Load test**: k6 or a Java harness; real numbers

### Left out (intentionally)
| Skipped | Why |
|---|---|
| Distributed tracing (OpenTelemetry/Jaeger) | Phase 4 |
| Alerting rules, SLO math | Phase 4 |
| Grafana dashboard polish | One basic dashboard max; optional |
| Log aggregation (Loki/ELK) | Overkill for now |
| Publishing a blog post series | One post only (see G6) |

### Depth ceiling
**Intermediate.** Emit the right metrics and read them. Don't build observability infrastructure.

### Micro-tasks
- [ ] **G1 (1h)** Micrometer gauges/timers for: `queue_depth`, `job_wait_seconds`, `job_run_seconds`, `jobs_failed_total`, `jobs_dead_total`, `lease_expired_total`. Expose `/actuator/prometheus`.
- [ ] **G2 (1h)** Add `jobId` / `workerId` to MDC and JSON log lines.
- [ ] **G3 (1.5h)** Load run: N workers, 10k jobs. Record throughput (jobs/s), p50/p95/p99 wait and run time. Put the numbers in the README.
- [ ] **G4 (1h)** Write `chaos-log.md`: one table, with every injected failure from Blocks B–F (kill -9, SIGTERM, SIGSTOP zombie, DB restart, handler crash after effect, etc.), *what happened*, *what you fixed*.
- [ ] **G5 (1h)** One extra chaos run: restart Postgres mid-load. Does the worker recover? Document it.
- [ ] **G6 (0.5h)** Draft **one** short post: "The bug that made me understand leases" (or similar).
- [ ] **G7 (1h)** Finalize README: problem → design → tradeoffs → "what I'd do differently" → how to run the tests.

### Deliberate failure
1. Restart Postgres while 4 workers are mid-flight. See what breaks (connection pool exhaustion? stuck workers?).
2. Fill the connection pool deliberately (pool size 2, 4 workers). Observe the *hang, not error* behavior from Phase 0.

### Closed-book check
- Why do p99s matter more than averages for queue wait time?
- Which single metric tells you the system is falling behind?
- List five failures you injected and what TaskForge did for each.

### Exit criteria (Phase 1 final gate)
- [ ] 4 workers + 1000 jobs → exactly-once side effects, automated test green.
- [ ] `kill -9` a worker mid-job → job recovers and completes elsewhere.
- [ ] You explain, **unprompted**, why exactly-once *delivery* is impossible and what you did instead.
- [ ] You can draw the job state machine on a whiteboard including **every illegal transition**.
- [ ] Metrics dashboard or `/actuator/prometheus` shows queue depth and latency histograms.
- [ ] `chaos-log.md`, `state-machine.md`, `tradeoffs.md`, `error-log.md` all exist and are honest.
- [ ] README opens with the problem, not the install steps.

---

## 3. Suggested week plan (flex it, don't worship it)

| Week | Blocks | Milestone |
|---|---|---|
| 1 | A + B | Jobs persisted, state machine enforced, stuck-job bug reproduced |
| 2 | C (first half) | Double-execution reproduced, `SKIP LOCKED` fix, leases |
| 3 | C (second half) + D start | **The 4×1000 test is green.** Retries/backoff begun |
| 4 | D finish + E | Jitter plots, graceful shutdown, idempotency |
| 5 | F + G | Cancellation, metrics, chaos log, README |

If you fall a week behind, cut **F2, F6, G5, G6** first. Never cut C7, D6, E4.

--- 

## 5. Progress tracker (copy into your notes)

```
[ ] A  Async foundations        started ____  done ____  hours ____
[ ] B  Job model & state machine started ____  done ____  hours ____
[ ] C  Safe claiming             started ____  done ____  hours ____
[ ] D  Failure handling          started ____  done ____  hours ____
[ ] E  Idempotency & delivery    started ____  done ____  hours ____
[ ] F  Scheduling & control      started ____  done ____  hours ____
[ ] G  Observe, prove, ship      started ____  done ____  hours ____
```

---

## 6. Prepares you for

- **CodeCrafts Deploy:** this code is nearly liftable as the build queue.
- **Distributed S3 Clone:** replication and GC queues run on this engine.
- **Interviews:** the exactly-once test, the lease/fencing story, and the jitter plots are your three talking points.

### Resume line (earn it by hitting the exit criteria)
> *Built a distributed job execution engine on Postgres using `SKIP LOCKED` for lock-free work claiming, with lease-based failure recovery, exponential backoff with jitter, and idempotent submission — verified exactly-once side effects under concurrent multi-worker load.*

---

## 7. Daily rhythm (optional, but it kills "where was I?")

1. **Start (5 min):** read the last line of yesterday's notes. Pick **one** micro-task.
2. **Work (1–3h):** one micro-task, one commit.
3. **End (5 min):** write what's done, what's next, and any open error in `error-log.md`.
4. **End of each block:** closed-book check on paper, then tick exit criteria honestly.

> When you're unsure what to build next: *build the failure.*
> When you're stuck: *what failure is this concept protecting me from?*
