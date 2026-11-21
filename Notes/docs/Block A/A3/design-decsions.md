# A3 — Design Decisions


## 1. Status Store and Work Queue Separation

**Decision:** The `ConcurrentHashMap` acts as the status store, while the thread pool's queue holds the work. The service puts the job in both.

**Reason:** The status store does not feed the thread pool. Reading a job's status never touches the work queue.

**Cost:** Two places must stay in sync. A bug between them can create ghost jobs or lost work (see Decisions 3 and 4).

## 2. Immutable Job Record

**Decision:** `Job(id, status, type)` is an immutable record. `updateStatus()` creates a new `Job` instead of modifying the existing one.

**Reason:** No thread can observe a partially modified job.

**Cost:** Every status update allocates a new object. This is acceptable at the current scale.

## 3. Save Before Execute

**Decision:** `submit()` performs all checks that can fail first, then calls `save()`, followed by `execute()`.

**Reason:** If a fast worker starts before the job is saved, `updateStatus()` returns `false`, and `runJob()` exits silently. The job remains stuck in `QUEUED`.

**Cost:** If `execute()` fails, the saved job must be rolled back (see Decision 4).

## 4. Remove Job When Pool Rejects Execution

**Decision:** If the executor throws `RejectedExecutionException`, call `store.remove(id)`.

- If the executor is shut down, throw `ExecutorShutdownException`.
- Otherwise, throw `QueueFullException`.

**Reason:** The client receives an error, so a `QUEUED` ghost job must not remain in the store.

**Proof:** Removing the `store.remove()` line caused two tests to fail.

## 5. Atomic Map Operations

**Decision:**
- Use `computeIfPresent()` for `updateStatus()`.
- Use `putIfAbsent()` for `save()`.

**Reason:** These operations are atomic at the map level.

- `computeIfPresent()` prevents a removed job from reappearing after a `get()` followed by a `put()`.
- `putIfAbsent()` prevents duplicate IDs and allows `DuplicateJobException` to return HTTP 409.

**Known Gap:** This does not prevent lost updates (last writer wins). Preventing those requires legal status transitions, which will be addressed in Block B.

## 5b. Bounded Queue with AbortPolicy

**Decision:**
- Core pool size: `2`
- Maximum pool size: `4`
- Queue capacity: `10`
- Rejection policy: `AbortPolicy`

**Reason:** Provides backpressure.

An unbounded queue never rejects tasks, prevents the pool from scaling beyond its core size under normal queueing conditions, and allows memory usage and waiting time to grow without limit.

**Proof:** Sending 16 POST requests resulted in:
- 14 accepted (`2` running + `10` queued + `2` additional workers).
- 2 rejected.

**Cost:** The client must handle HTTP 503 responses.

## 6. Queue Full Returns HTTP 503, Not 429

**Decision:** Return `503 Service Unavailable` when the queue is full.

**Reason:** The problem is server capacity, not an individual client's request rate.

**Note:** HTTP 429 would be appropriate if the limit were enforced per client. Changing this later requires updating the test and exception handler.

## 7. HTTP 202 Accepted with Location Header

**Decision:** POST returns:
- HTTP `202 Accepted`
- A job ID
- A `Location` header pointing to `/api/jobs/{id}`.

**Reason:** The work has been accepted but is not yet complete. The client needs a URI to check its status.

`ServletUriComponentsBuilder` constructs the URI so that it does not drift from the controller mapping.

**Open Decision:** The current submission endpoint is:

`POST /api/jobs/post-job`

`POST /api/jobs` would communicate the same meaning because POST already implies resource creation.

Choose one path and document the decision.


## 8. Worker Exceptions and Controller Advice

**Decision:** Worker threads do not use the controller advice. They report failures through job status updates and logs.

**Reason:** No HTTP request is waiting for the worker to finish. Therefore, controller advice has no request context in which to handle worker exceptions.

## 9. Throwable Handling in runJob()

**Decision:** `runJob()` catches `Throwable` and performs the following steps:

1. Logs the failure.
2. Updates the job status to `FAILED`.
3. Rethrows the exception if it is an `Error`.

**Reason:** A job must not remain stuck in `RUNNING` because of an `Error`.

The status update happens before rethrowing. Rethrowing ensures serious JVM errors are not silently hidden.

**Cost:** The application may continue running even when the JVM is in an unhealthy state. This is accepted for now.

## 10. JsonNode Payload Handling

**Decision:** `SubmitJobRequest` contains:
- `type`
- `JsonNode payload`

The service identifies the handler, converts the payload into the expected type, and validates it.

**Reason:** Jackson cannot directly construct an arbitrary `JobPayload` interface implementation.

This approach ensures invalid payloads become `InvalidPayloadException` (HTTP 400) instead of unexpected HTTP 500 errors.

**Cost:** The compiler cannot guarantee that a handler and its payload type match. Runtime class validation handles this.

## 11. Handler Registry

**Decision:** `JobHandlerRegistry` maps each `JobType` to its corresponding handler.

A new job type requires only a new handler class.

**Open Decision:** An unknown job type currently throws `IllegalArgumentException`. Map this explicitly to HTTP 400.

## 13. EchoPayload Delay Limit

**Decision:** Set the maximum delay in `EchoPayload` to `9000 ms`.

**Reason:** A client should not be able to hold a worker thread for an excessively long time.

## 14. Jackson 3 Only

**Decision:** Use Jackson 3 (`tools.jackson`) consistently throughout the application.

**Reason:** Mixing Jackson 2 and Jackson 3 caused request parsing failures.

---

## Design Summary

The A3 implementation establishes an in-memory asynchronous job execution system with:

- Separate status storage and work queue.
- Immutable job records.
- Atomic map operations.
- Bounded thread pool and backpressure.
- Explicit rejection handling.
- Asynchronous HTTP submission using `202 Accepted`.
- Runtime payload conversion and validation.
- Centralized job status tracking.

The major architectural limitation is that the system is entirely in-memory. Jobs disappear when the application restarts.

This limitation becomes the primary motivation for Block B.