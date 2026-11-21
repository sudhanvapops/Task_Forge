- 429 vs 503: 
    429 means "you sent too many". 
    Our queue is full because of the server, 
    so 503 fits better.

- 202 not 200: 
    the job is only accepted, not done. 
    202 + Location says "check later here".

- Bounded vs unbounded queue: 
    bounded (10) means rejections but memory stays safe. 
    Unbounded never rejects, 
    but wait time and memory grow forever. 
    Bounded gives backpressure.

- Throwable rethrow: 
    I mark the job FAILED, 
    then rethrow Errors (like OutOfMemoryError) s
    o serious problems are not hidden. 
    The pool replaces the dead worker.

- Delay cap (9000 ms): 
    stops one request from holding a worker for too long.

- /post-job vs POST /jobs: 
    I used /post-job early. 
    REST style is POST /api/jobs. 
    Change it later.