### TIL

- The pool fills the queue BEFORE it grows past core size.
- A thread created because the queue was full runs the new task directly.
- With an unbounded queue, max size is never used.
- println from several threads interleaves. One call per line fixes it.
- Order matters: 
    record the job BEFORE handing it to another thread.
    Otherwise the worker can run ahead of my bookkeeping.

## Executor family (how the pieces relate)

Executor (interface)
  - one method: execute(Runnable). "Run this task somehow."
  - Executor says WHAT to do, not HOW.

ExecutorService (interface, extends Executor)
  - adds: submit(), shutdown(), awaitTermination()
  - submit() returns a Future, so you can check the result later.
  - This is the type you normally program against.

ThreadPoolExecutor (class, implements ExecutorService)
  - the real pool: worker threads + a queue + a rejection policy.
  - Settings: core size, max size, keep-alive, queue, ThreadFactory,
    rejection handler.

ThreadFactory (separate interface, NOT part of the chain above)
  - one method: newThread(Runnable). Creates threads for the pool.
  - I use it to give threads readable names (job-worker-N).

JobExecutorFactory (MY class)
  - does not extend anything. It only builds a configured
    ThreadPoolExecutor and returns it.
  - Benefit: pool settings live in ONE place. A3 and A4 reuse it.