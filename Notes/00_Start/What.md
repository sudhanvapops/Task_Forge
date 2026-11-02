What you’re building (the big picture)

TaskForge is a service you hand slow work to (“build this repo”, “send these emails”), and it does the work in the background while you get an answer immediately. Later, CodeCrafts Deploy uses it so every deployment is a job.

Think of a restaurant:

Waiter takes your order and hands you a ticket number right away. This is POST /jobs, which returns 202 and a job id.
Kitchen cooks the orders in the background. This is a pool of worker threads.
Ticket rail is where orders wait if the cooks are busy. This is the queue. It has a limit, because a rail that holds infinite tickets eventually collapses.
Status board shows “waiting / cooking / ready”. This is GET /jobs/{id}, backed by a ConcurrentHashMap.
Recipe book says how to cook each dish type. This is the JobHandler interface, one handler per job type.
client --POST /jobs--> controller --> queue --> worker thread --> JobHandler
   ^                       |                         |
   |<--202 + id + Location-+                         v
   +--------GET /jobs/{id}----- status board (ConcurrentHashMap) <--- status updates

