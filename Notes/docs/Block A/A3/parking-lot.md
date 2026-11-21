- Validator branch not covered:
    have to learn what that piece code does
    EchoPayload checks in its constructor, not with annotations.

- The 500 response leaks exception.getMessage().

- Unknown URL returns 500, not 404.
- WorkerPoolStartupCheck uses System.out; replace with a logger.
- shutdown() does not wait for queued jobs (A4 may fix this).
- No error field on Job.
- PriorityBlockingQueue is unbounded.
- Unknown job type throws IllegalArgumentException, not mapped to 400.
- Lost update vs legal status transitions: state machine in Block B.