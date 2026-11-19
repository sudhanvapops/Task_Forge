### ThreadPool Executor

What is happening here is 
ThreadPool Executor is acting as oth:
    Queue
    thread maneger

and the worker threads that are being created ther eis by
ThreadFactory

In block b we use Postgress as a Queue

### Rejection Policy

We use default one to give the execute() function a Rejection
later to turn it into clean Http Code Rejection

429: too many requests
503: service Unavvailable but backend is running
server is running but cannot respond right now.

### Thread Factory

is an interface in Java that defines how new threads should be created
having a separate factory gives us control over thread creation.

using this cause to debug properly which service worker got error
defult it will name: 
    pool-1-thread-3

Whenever the executor needs a new worker thread, it asks the ThreadFactory to create one.

The control We can get:
    Cutom Name
    Prioirty
    Configure Demon Thread

### How Thread are creted
Suppose you submit tasks one by one.
1. First task arrives → Executor requests a thread from ThreadFactory.
2. Second task arrives → Executor requests another thread.
3. Third task arrives → Both core threads are busy, so the task goes into the queue.
4. More tasks arrive → They are queued.
5. Queue becomes full → Executor can request additional threads, up to the maximum of 5.

### Total

ThreadPoolExecutor = Restaurant manager
Worker threads = Employees
Tasks = Customer orders
ThreadFactory = HR department that recruits and prepares employees
