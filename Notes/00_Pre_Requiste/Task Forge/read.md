Phase 1 is now broken into 7 ordered blocks in Topic.md, A through G, about 55 hours in total. Each block has the same fields: goal, what it covers, what’s left out and why, depth ceiling, micro-tasks, a deliberate-failure drill, a closed-book check, exit criteria, and a time box.

Block	Focus	Time
A	Thread pools, the 202 pattern, in-memory queue	~6h
B	Postgres persistence, enforced state machine, @Version	~7h
C	SKIP LOCKED, leases, heartbeats, fencing	~12h
D	Backoff, jitter, dead-letter table, graceful shutdown	~10h
E	Idempotency keys, at-least-once reasoning	~6h
F	Priority, delayed jobs, cancellation, claim index	~8h
G	Metrics, chaos log, README	~6h

The source roadmap’s 8 build steps are all in there, regrouped so each block creates the problem the next one solves. Each block has micro-tasks of 1–3 hours with IDs like C1 to C8, so you always have one small next thing to pick up.

Since you’re doing this solo, I added a few things the roadmap didn’t have:

Anti-loop rules: a 90-minute stuck rule, a one-day rule before asking for a hint, a parking-lot.md for tempting tools, and a rule to cut scope at 50% overrun.
Scope freeze: TaskForge is only submit, claim, run, record and survive failure. No UI, auth, cron or multi-tenancy.
Docs to keep from day one: a repo skeleton with error-log.md, chaos-log.md, tradeoffs.md and state-machine.md, so the README and your interview stories already exist by the end.
Cut order: if you fall behind, the week plan says what to drop first (F2, F6, G5, G6) and what never to cut (C7, D6, E4).
A test inventory: the tests that matter for each block, in one table.

Block C is the core of the phase, so protect its 12 hours. Start with A0 (repo skeleton plus a Testcontainers smoke test), which takes about an hour.