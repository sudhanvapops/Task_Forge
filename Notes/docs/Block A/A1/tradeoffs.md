JobType enum instead of a string
- Gain: unknown types fail at JSON parsing (clean 400) before any worker.
- Cost: every new job type edits the enum.

Typed payload records instead of Map<String,String>
- Gain: compile-time field checks, real number types, validation in one place.
- Cost: one small class per job type.

Generics on JobHandler<P,R> plus payloadType()
- Gain: each handler declares its payload type and the compiler enforces it.
- Cost: the registry holds JobHandler<?,?>, so the specific type is lost there.
- Mitigation: executeUntyped casts once, inside the interface, using Class.cast.

executeUntyped on the interface instead of a cast in the worker
- Gain: one guarded cast point; a clear ClassCastException on mismatch.
- Cost: the interface carries one extra default method.

Package by feature
- Gain: the folder tree shows the architecture; a new job type means new files only.

Unit tests without Spring vs @SpringBootTest
- Gain: registry tests run in milliseconds; context tests never touch the dev DB.
- Cost: two test styles to maintain.