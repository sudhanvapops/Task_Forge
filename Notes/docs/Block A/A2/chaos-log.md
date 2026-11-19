## A2: unbounded vs bounded queue (10,000 jobs)

- Unbounded (LinkedBlockingQueue): heap went from 30 to 45 MB in
  jconsole. 
  Threads stayed at 17.

- Bounded (ArrayBlockingQueue(10)): 
    heap stayed around 35 MB. Rejections
    started at task 15.

- Why: because the maxpool size hit and triggred Rejection: abort policy

- Evidence: skipped screenshot (SKIP EVIDENCE).