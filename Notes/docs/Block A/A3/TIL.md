### TIL: TaskForge A3

## Thread pool

A ThreadPoolExecutor fills in this order: 
    core threads first, then the queue, then extra threads up to max, then reject.
    Proof: 
        core 2, queue 10, max 4 gave 14 accepted (2 + 10 + 2). 16 POSTs left 2 rejected.

Extra threads die after the idle time. 
All workers die after the app stops (destroyMethod="shutdown").

An unbounded queue is never full, 
so extra threads never start and nothing is ever rejected. 
A bounded queue gives backpressure: the service says "503, try later" and stays healthy.

## Races in the store

Ghost update: 
    get, then another thread removes, 
    then put brings the removed job back. 
    computeIfPresent does check and update in one step, 
    so it cannot happen.

Lost update: 
    two threads write, 
    and the last writer wins (FAILED over SUCCEEDED). 
    computeIfPresent does not fix this. 
    Legal status transitions do (state machine, Block B).

The test that catches the ghost bug 
is the multi-thread one (update racing with remove), 
not the single-thread "unknown id" tests.

## Order of work in submit()

Save to the store first, 
then execute. 

If a fast worker runs before the save, 
updateStatus returns false, 
runJob returns quietly, and the job stays QUEUED forever. 

There is no error, only silent lost work.

If the pool rejects the job, 
remove it from the store. 
Without that, the client gets a 503 
but a ghost QUEUED job stays in the map. 

### Errors

catch (Exception) does not catch an Error (like StackOverflowError). 
The job would stay RUNNING forever. 
The app does not crash. 
Only that worker thread dies, and the pool starts a replacement.

Fix: catch Throwable, log it, set FAILED, and then rethrow if it is an Error. The status update must come before the throw.

## HTTP and Spring

202 means "accepted, not finished". 
The Location header says where to check. Build it with ServletUriComponentsBuilder, not by hand.

@Valid alone is not enough. 

Two advice classes need @Order, or the catch-all Exception handler wins and everything becomes 500. 
This was the "all 500" bug.

Spring Boot 4 uses Jackson 3 (tools.jackson). 
A Jackson 2 JsonNode or ObjectMapper breaks request parsing. 
Test annotations moved packages in Boot 4 (@WebMvcTest, @MockitoBean).

## Testing

Do not use Thread.sleep. 
Use a latch as a "gate" to hold the handler, and shutdown() plus awaitTermination to wait for all jobs.
spy means real behavior, but I can still verify the calls.
A passing test does not mean it guards the code. The drill (break the line on purpose) shows which tests really watch it.
A test can also be out of date. My delay-limit test failed because I had raised the limit to 20 seconds for Postman.

## About my work and my learning

Restart loses everything, and GET then returns 404. 
The system denies a job it accepted. 

Block B fixes this with Postgres as the status board and the queue, 
and leases for jobs that were RUNNING when the app died.

Test-driven works better for me: 
    Claude writes the tests, 
    I write the logic, and I rewrite the tests myself later.

When I answer "what happens", 
I should trace it in steps: 
    which thread, which line, what state.

Silent bugs are harder than loud ones. 
    Feeling small about that is normal. 
    It is the move from CRUD to systems.
    
A3 took about 8 hours.