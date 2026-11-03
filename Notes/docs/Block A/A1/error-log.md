### A0

Test passed while connecting to the dev DB. 
Cause: @ServiceConnection missing, so Spring fell back to spring.datasource.*. 
Fix: restore the annotation, and check the Flyway log port.

With the annotation removed and Compose down: “Connection to localhost:5433 refused”. 
Cause: nothing at the fallback address.
Initializr’s TaskForgeApplicationTests depends on the Compose DB. Cause: no Testcontainers setup. 
Fix: delete it or give it the same setup.

