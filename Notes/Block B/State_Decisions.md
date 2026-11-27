### State Decisisons

This is an administrative transition, not an ordinary worker transition.

SUCCEEDED: Terminal.
CANCELLED: Terminal.
DEAD: Terminal for automatic processing
        manual administrative recovery is permitted.


Retry to Canceld legal
if both a worker and cancel arrive at the same time
cancel should have priority

X -> X error
means running -> running not allowed
rejecting it is a reasonable choice 
because it helps expose duplicate events and invalid state changes
but if the worker is completed the job 
but you sed cancel at updatig the db state 
then what to do
OPtimistic lockign helps here to decide who wins the job


Also: A retry -> Queued -> Running
means: a second worker doesnt need to know the max attempts and all the details
it just puts to queue the worker will pick it

The worker claims either type of job.
This means the worker needs to understand both states.


### Decisons

Important design decisions
Question	Decision
FAILED or DEAD?	DEAD represents permanent failure
CANCELLED?	Separate state for intentional cancellation
RETRYING → CANCELLED?	Legal
DEAD → QUEUED?	Manual administrative operation
X → X?	Reject as an error
Terminal states?	SUCCEEDED and CANCELLED strictly

### Dought

Why not move retyring to queue then running
its like saying a worker can claim jobs from retrying iteslef 
so are we implementing 
retrying to running by another worker
or normal queing process where the worker picks it up
