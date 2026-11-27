### TIL: A4 (Spring ThreadPoolTaskExecutor)

ThreadPoolTaskExecutor is a Spring wrapper around the same ThreadPoolExecutor. 
The pool rules are the same as in A2.

The wrapper builds the real pool only after initialize(). 
Spring does this for a bean. 
In a plain test I must call it myself.

The default queue capacity is unbounded. 
A pool only adds threads above core size when the queue is full. 
So with the default queue, max size and the rejection handler never do anything.

Same test as A2 (core 2, max 4, queue 10, 16 jobs): 14 accepted, 2 rejected.

Spring throws TaskRejectedException. 
It is a subtype of RejectedExecutionException, so my catch block still works.

Keep the wrapper as the bean. 
Its shutdown settings only work if Spring manages the wrapper, 
not the inner pool.

Graceful shutdown (setWaitForTasksToCompleteOnShutdown plus setAwaitTerminationSeconds) waits only up to the timeout. 
14 jobs of 9 s on 4 workers is about 36 s, so a 30 s timeout loses the last ones.


mvn spring-boot:run kills the app about 5 s after Ctrl+C, so it cannot test graceful shutdown. Use java -jar.

Removing the wait setting made my shutdown test fail (expected 3, got 0). That proves the test guards the setting.

An in-memory job store loses everything on restart. The client got 202, then GET returns 404.

