1. Handler and registry were not Spring beans
   - What: EchoJobHandler and JobHandlerRegistry had no annotation, so Spring ignored them.
   - Why: the container only injects beans it knows about; the failure shows up late, at lookup.
   - Fix: annotated both. Proved with a @SpringBootTest that fetches ECHO from the registry.

2. Wrong prediction: handler.execute(payload) on JobHandler<?,?> compiles
   - Reality: it does not (wildcard capture).
   - Why: the registry holds different payload types, so P is unknown. Allowing it would let a
     BuildPayload reach the echo handler.
   - Fix: payloadType() returns Class<P>, and executeUntyped(JobPayload) casts inside the interface.

3. Test-only value JobType.UNKOWN in the production enum
   - Why bad: production code carried a type that exists only for tests, and a client could send it.
   - Fix: removed it. The test uses new JobHandlerRegistry(List.of()) and looks up ECHO.

4. EchoPayload accepted bad data
   - Primitive delay silently became 0 on a missing field, negative values break Thread.sleep
     inside a worker, and message could be null.
   - Fix: compact-constructor validation; renamed delay to delayMs (units in the name).
