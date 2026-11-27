### States

(non terminal state)    QUEUED: Job is in the queue
(non terminal state)    RUNNING: A worker picked it and working on it
(non terminal state)    RETRY: A worker failed to do it so now it is kept for retry
(terminal state)        SUCCEEDED: A worker finshsied job succesfully
(terminal state)        CANCELLED: A manual Request to cancel the Job
(terminal state)        DEAD: Max attempt exceeded or An error happend while proceesing a job


### Matrix

From ↓ / To →	QUEUED	RUNNING	RETRYING	SUCCEEDED	CANCELLED	DEAD
QUEUED	            ✗	    ✓	    ✗	        ✗	    ✓	        ✗
RUNNING	            ✓	    ✗	    ✓	        ✓	    ✓	        ✓
RETRYING	        ✗	    ✓	    ✗	        ✗	    ✓	        ✗
SUCCEEDED	        ✗	    ✗	    ✗	        ✗	    ✗	        ✗
CANCELLED	        ✗	    ✗	    ✗	        ✗	    ✗	        ✗
DEAD	            M	    ✗	    ✗	        ✗	    ✗	        ✗


M: Manual



### LEGAL

QUEUED    -> RUNNING      worker claims the job
QUEUED    -> CANCELLED    user cancels before execution

RUNNING   -> SUCCEEDED    handler completes successfully
RUNNING   -> CANCELLED    cooperative cancellation
RUNNING   -> RETRYING     handler fails; attempts remain
RUNNING   -> DEAD         permanent error or attempts exhausted
RUNNING   -> QUEUED       lease expires; reaper requeues job

RETRYING  -> RUNNING      retry becomes due; worker claims job
RETRYING  -> CANCELLED    user cancels while waiting for retry

DEAD      -> QUEUED       administrator manually requeues job



### Illegal

ILLEGAL (and what breaks)

SUCCEEDED -> anything     completed job must never execute again

CANCELLED -> anything     cancelled job must never resume

DEAD      -> RUNNING      bypasses administrative recovery

QUEUED    -> SUCCEEDED    skips execution; false completion

QUEUED    -> DEAD         skips normal execution/failure handling

RETRYING  -> SUCCEEDED    job succeeds without another execution

SUCCEEDED -> RUNNING      duplicate execution and side effects

CANCELLED -> RUNNING      ignores user's cancellation request

DEAD      -> SUCCEEDED    claims success without execution

X         -> X            error; duplicate transition or event