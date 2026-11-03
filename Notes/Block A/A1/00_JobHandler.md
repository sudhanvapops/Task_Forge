### Job Handler

when req ia submited into to queue
the worker picks it up from queue

a worker doent know how to perform specific tasks
    how to send pdf
    how to send email
    how to send sms

but it should know how to execute a job

So we are making a Interface named JobHandler
which contains execute method

A Handler: lets say PDFJobHandler
knows how to send pdf 
it should impmement JobHandler interface

the worker will simply call
jobhandler.execute(job)


What it applies
    Liskovs Open Close principle

What it prevents
    a gaint nest of if/switch statments
    and modifying again and again a class

This creates two problems and we want to avoid it:
    The worker becomes responsible for understanding every job type.
    Adding new functionality requires modifying existing code.


### JobRegistry

knows how to choose what handler to give for worker to perform that task


### The Worker

The worker only knows how to:
    1. Get the job type.
    2. Find its handler.
    3. Ask the handler to execute the job.

The worker doesnt care how the job gets executed

