link: `https://claude.ai/chat/f8b15cf8-c2ab-4a29-8238-444f3efdd0ef`


status.put(id, "QUEUED");       // 1: entry exists before any worker can touch it
try {
    pool.execute(() -> {
        status.put(id, "RUNNING");     // 2: worker thread starts the work
        try {
            resize(req);
            status.put(id, "SUCCEEDED"); // 3: work finished fine
        } catch (Exception e) {
            status.put(id, "FAILED");
        }
    });
} catch (RejectedExecutionException e) {
    status.remove(id);          // 4: undo, this job never got accepted
    return ResponseEntity.status(429).body("Busy, try later"); // 5: queue full
}
return ResponseEntity.accepted().location(URI.create("/resize/" + id)).build();