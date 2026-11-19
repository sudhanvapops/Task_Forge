## A2: pool settings
- Core 2, 
    max 4, 
    keep-alive 30s, 
    ArrayBlockingQueue(10), 
    AbortPolicy

- Why bounded queue: 
    an unbounded queue never fills, so the pool never
    grows past core and memory grows without limit.

- Why AbortPolicy: it is the only policy where the caller finds out.
  The others hide the failure (___ say why for each).

- Thread names: 
    job-worker-N from an AtomicInteger, 
    so thread dumps are readable. 
    The name belongs to the thread not the task.

- All pool settings (core, max, queue, rejection policy, thread names)
  are in one place.
- The controller only uses the pool. It doesn't know how it was built.
- Tests can build a small pool easily.
- In A4 these settings move into a Spring bean. Since they were in one
  place, I can compare A2 and A4 line by line.