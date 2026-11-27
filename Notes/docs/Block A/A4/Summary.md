### A4
I swapped my hand-built pool for Spring's ThreadPoolTaskExecutor and got the same results (14 accepted, 2 rejected). 
The queue controls when the pool grows. Graceful shutdown works only within its timeout. 
The in-memory store loses accepted jobs on restart, which is why Block B moves jobs to Postgres.

### A5
POST returns 202, not 200, because the job is only accepted. 
The client polls the Location URL until the job is done.

