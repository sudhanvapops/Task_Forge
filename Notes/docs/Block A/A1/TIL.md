### TIL: Today I Learned

- Strategy pattern: 
    a registry picks behaviour by key, so new types add files instead of edits.

- Modelling from a story: 
    underline the nouns (types) and verbs (methods), then decide which noun owns which verb.

- Marker interface vs generics: 
    the marker groups types
    generics give the type safety.

- Type erasure and wildcards: 
    why JobHandler<?,?> can't call execute(payload).

- Class<P> and Class.cast: 
    a runtime-checked cast done in one place.

- Records and compact-constructor validation.

- Fail loudly: 
    duplicate keys throw at startup instead of silently replacing a handler.

- JUnit: assertThrows, assertInstanceOf unit tests without Spring.
- A green test can hide which DB it ran against check the port in the logs.
- Workflow: write the plain-English plan first, then implement, then show evidence.