- Every error returned 500: 
    the catch-all advice ran first. 
    Fix: @Order(1) on the specific advice.

- Jackson 2 vs 3 mix: 
    Boot 4 uses tools.jackson, 
    not com.fasterxml. 
    Wrong import = compile or runtime errors.

- Ghost job: 
    queue full but job stayed in the store. 
    Fix: store.remove() in the reject catch. 
    Two tests turned red when I removed it.
