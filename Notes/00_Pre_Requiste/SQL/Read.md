
I made Topic_SQL.md. It has seven SQL blocks, SQL-A to SQL-G, one for each TaskForge block, and about 16 hours in total. Each block uses the same fields as Topic.md (goal, covers, left out, depth ceiling, micro-tasks, deliberate failure, closed-book check, exit criteria), and tasks are numbered S-A1, S-C4 and so on so they don’t clash with the main task IDs.

SQL block	Learn	Time	Pairs with
SQL-A	psql, SELECT/INSERT/UPDATE/DELETE, joins	~4h	Block A
SQL-B	DDL, constraints, transactions, optimistic lock in raw SQL	~3h	Block B
SQL-C	FOR UPDATE, SKIP LOCKED, the claim query, fencing, the reaper	~5h	Block C
SQL-D	Intervals, retry updates, atomic dead-letter move	~2.5h	Block D
SQL-E	ON CONFLICT, upserts, the RETURNING gotcha	~2h	Block E
SQL-F	EXPLAIN ANALYZE, partial indexes, the cancel race	~3h	Block F
SQL-G	percentile_cont, pg_stat_activity	~1.5h	Block G

Some choices to know about:

Every query starts in psql. Core queries then go into Java as native SQL, not Hibernate, since Hibernate hides exactly what you’re trying to learn.
The claim query is described by its three parts, not written out. In S-C4 you write it yourself, because that’s where the learning is.
Each block has its own failure drill. For example, you’ll cause a double-claim with the naive two-step query before you fix it.
A final gate on paper at the end. It covers the claim query, the idempotent insert and the atomic dead-letter move, written from memory with no AI.
Cut order if time is short: drop the ⭐ tasks first. Never drop SQL-C or the closed-book checks.

Start tonight with S-A1 and S-A2 (connect to psql, then the pgexercises Basic section), about an hour and a half. That fits alongside TaskForge A0.