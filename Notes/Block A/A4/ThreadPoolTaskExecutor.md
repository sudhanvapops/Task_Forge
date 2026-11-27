### ThreadPoolTaskExecutor

it is not a ThreadPool 
it initilaizes one


is a Spring abstraction that manages a pool of reusable threads to execute tasks asynchronously.

that wraps Java's ThreadPoolExecutor as a Spring bean for managing asynchronous task

The @Async annotation tells Spring to execute this method asynchronously using a task executor.


Same @Async uses proxy
so calling it by another method 
wont act as intended


uses LinkedBlockingQueue by default, 
but you can configure its queue capacity.

+ve LBQ
0 or -ve Syncronus queue

@EnableAsync 
    is a Spring annotation that activates Spring's ability to run methods marked with @Async in background threads instead of blocking the caller.