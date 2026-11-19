## A2: shutdown() inside the loop
- What happened: Only task 1 was accepted. Tasks 2-15 were rejected.
- Cause: I called pool.shutdown() inside the for loop. After shutdown the
  pool accepts nothing new.
- Fix: moved shutdown() to after the loop.
- Lesson: AbortPolicy rejects in two cases: pool full AND pool shut down.
  Both throw the same RejectedExecutionException, so the exception alone
  doesn't say which case it was.

## A2: interrupt handler shut down the whole pool
- I called pool.shutdown() in the InterruptedException catch. One
  interrupted worker should not stop the whole pool.