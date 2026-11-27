Block B orientation: Job Model & State Machine

I read output.txt first. Your Block A state matches the resume note.

Big picture

Analogy: a parcel-tracking system.

Each parcel has one current status, and only certain moves are allowed. "Delivered" can never go back to "In transit".
Every scan is written to a history log, so you can replay what happened.
If two clerks edit the same parcel at once, a version stamp makes the second one fail instead of silently overwriting the first.
If the delivery truck burns down, the parcel stays "Out for delivery" forever, because nobody is left to update it.

Today your jobs are in a ConcurrentHashMap. Block B moves them to Postgres and adds the rules.

POST /api/jobs/post-job
   -> JobService.submit
        -> save job row (QUEUED) + event row      [Postgres]
        -> jobExecutor.execute(runJob)            [still pushed, like Block A]
              runJob (worker thread)
                -> transition QUEUED -> RUNNING
                -> handler runs
                -> transition RUNNING -> SUCCEEDED / FAILED

transition(jobId, from, to), ONE DB transaction:
   1. JobTransitions says: is from->to legal? (else throw)
   2. UPDATE jobs ... WHERE id=? AND version=?   (@Version)
   3. INSERT into job_events (from, to, timestamp)

Where each task fits:

B1 is the rulebook on paper.
B2 is the tables.
B3 is the rulebook in code.
B4 is the transaction that changes the row and writes the history together.
B5 swaps the map for Postgres.
B6 adds the version stamp.

Depth ceiling: Production on state machines, Intermediate on optimistic locking. Left out: Spring Statemachine, event sourcing, JSON queries on payload.

Claims to verify against your code
Many tests are not running. In JobControllerTest, JobHandlerTests, JobHandlerRegistryTest, TFSmokeTest and JobServiceTest, the @Test and @SpringBootTest lines are commented out. "All green" only covers InMemoryJobStoreTest, ExecutorConfigTest and JobServiceSubmitTest. Check with mvn test and look at the test counts.
Testcontainers. Your pom pins 1.21.4, but Spring Boot 4.x manages Testcontainers 2.x, which renames some packages and artifacts. Settle this before B6 with mvn dependency:tree | findstr testcontainers.
The WorkerPoolStartupCheck double-count bug is already fixed. It only prints getQueueCapacity().
B5 says "executor pulls from DB". That is Block C (SKIP LOCKED), so I think the line is wrong. In B5, submit should still push into the executor. Only the store changes.
Restart also strands QUEUED jobs, not only RUNNING ones. Note that in your error log.
Reading list (search terms, no URLs)
Spring Framework reference, "Declarative transaction management", and the "proxy / self-invocation" caveat.
Spring Data JPA reference, "Transactionality" and "Locking".
Hibernate User Guide, "Optimistic locking" and @Version.
PostgreSQL docs, "CHECK constraints" and "Multicolumn indexes".
Flyway docs, "Migrations" and naming (V2__...).
Video: Vlad Mihalcea, "optimistic locking @Version" (I'm not sure of the exact title, so search the channel).
Task plan (all start at Level 2)
B1. Draw the diagram and list every legal and illegal arrow. Decide CANCELLED now, and decide whether RETRYING and DEAD stay in the enum.
B2. Write V2__jobs.sql. Flyway checks that your entity matches the schema (ddl-auto: validate), and status needs a CHECK constraint.
B3. The pieces are Map<JobStatus, Set<JobStatus>> and EnumMap. Design question: what does an illegal transition throw, and which HTTP code is it if it ever reaches a controller?
B4. Find where @Transactional has to live. Your current runJob calls setStatus inside the same class. Find out why that matters.
B5. The Job record has no payload, but the table does. Decide the record or entity shape and how payload is saved.
B6. Find the Spring exception that wraps a JPA optimistic-lock failure.




