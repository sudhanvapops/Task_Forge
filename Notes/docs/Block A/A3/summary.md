### A3 Summary

Goal: POST returns 202 with an id and Location. GET returns the status from an in-memory map. A full queue gives a clean 503 and the app survives.

Built: the controller, getJob, the exception handlers, and a pool with core 2, max 4 and a queue of 10.

Status flow: QUEUED, then RUNNING, then SUCCEEDED or FAILED.

Errors: 
    400 for bad input, 
    404 for not found, 
    409 for a duplicate, 
    503 for a full queue or a shut-down pool, 
    500 for the rest.

Pool proof: 16 POSTs with 9 second jobs gave 14 accepted and 2 rejected.

Pool cleanup: extra workers die after the idle time, and all workers die after the app stops.

Restart proof: after a restart, GET returns 404 for every job. The system denies jobs it accepted.

Bug 1: everything was 500 because the advice classes had no @Order.
Bug 2: requests failed to parse because Jackson 2 was mixed with Jackson 3.
Bug 3: the body was empty because @RequestBody was missing next to @Valid.
Bug 4: a ghost job stays in the map if the pool rejects it and the job is not removed.
Bug 5: a job stays RUNNING forever if an Error is thrown and only Exception is caught. Fixed with Throwable, set FAILED, then rethrow.

Closed-book, solid: where the job lives after 202, bounded vs unbounded queue, and why lost jobs on restart are bad.
Closed-book, shaky: ghost update vs lost update, save-before-execute, and what an Error does.

Revisit plan: trace "what exactly happens" in steps (which thread, which line, what state) before answering.

Time: about 8 hours.