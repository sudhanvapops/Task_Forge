D0. Used Stratergy Pattern To Handle Multuple Job Types

D1. Handlers are keyed by JobType and discovered by Spring.
    JobHandlerRegistry takes List<JobHandler<?,?>> and builds an unmodifiable map.

D2. Duplicate type -> IllegalStateException at startup (app refuses to boot).
    Unknown type -> IllegalArgumentException listing the registered types.

D3. Payloads are typed records that implement the JobPayload marker interface.
    Validation lives in the record's compact constructor.

D4. Each handler exposes payloadType() (Class<P>).
    Needed now for the safe cast, and in Block B to turn stored JSON back into the right class.

D5. The worker will call only executeUntyped(JobPayload).
    The cast happens inside JobHandler, where P is known, in one place.

D6. Unknown job types should be rejected at POST /jobs, not inside a worker.
    The client gets an immediate error instead of an accepted job that dies later.

D7. Registry logic is tested with plain unit tests; anything needing the Spring context uses Testcontainers.

D8. Package layout: job/ (contracts + registry), job/handler/ (concrete handlers + their payloads).