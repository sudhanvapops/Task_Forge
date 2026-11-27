### 200 vs 202

- 202 means "I accepted your job", not "I finished it". 200 would say the work is done, and it is not.
- The client reads the Location header and polls GET /api/jobs/{id} until the status is SUCCEEDED or FAILED.
- Jobs can take minutes, but HTTP requests time out. 
So we return fast with an id and do the work in the background.


### Diffrence

A2: plain ThreadPoolExecutor	A4: ThreadPoolTaskExecutor
Thread names	job-worker-N via my own ThreadFactory	job-worker-N via setThreadNamePrefix
Rejection exception	RejectedExecutionException	TaskRejectedException (a subtype, so my catch still works)
Shutdown	shutdown() only. Does not wait for the caller.	Waits for running and queued jobs, up to the timeout (30 s). Jobs beyond that are lost.
Setup	Constructor with 7 arguments	Setters, plus an initialize step that Spring runs for a bean
No queue capacity set	Not possible (the constructor needs a queue)	Default queue is unbounded, so the pool never grows past core and nothing is rejected
Same result (16 POSTs, 9 s jobs)	14 accepted, 2 rejected	14 accepted, 2 rejected