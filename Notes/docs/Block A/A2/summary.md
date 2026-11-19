### A2 summary

- Built JobExecutorFactory. It builds and returns a ThreadPoolExecutor
  with: core 2, max 4, keep-alive 30s, ArrayBlockingQueue(10),
  named threads (job-worker-N), AbortPolicy.
- Pool order: core threads first, then the queue fills, then extra
  threads up to max, then rejection.
- Unbounded queue: never full, so the pool stays at core size, max is
  unused, and the heap grows until it crashes.
- Bounded queue: memory capped, extra jobs are rejected.
- AbortPolicy also rejects after shutdown(). That was my bug.
- Decision for A3: put the map entry in first (QUEUED), then execute().
  On rejection, remove the entry and return 429/503.