From ↓ / To →   QUEUED  RUNNING  RETRYING  SUCCEEDED  CANCELLED  DEAD
QUEUED            ✗       ✓        ✗         ✗          ✓         ✗
RUNNING           ✗       ✗        ✗         ✓          ✓         ✓
RETRYING          ✗       ✗        ✗         ✗          ✗         ✗
SUCCEEDED         ✗       ✗        ✗         ✗          ✗         ✗
CANCELLED         ✗       ✗        ✗         ✗          ✗         ✗
DEAD              ✗       ✗        ✗         ✗          ✗         ✗


Each arrow is tagged with the block that uses it:

Arrow	                        Block	            Note
QUEUED -> RUNNING	                B	      worker starts the job
QUEUED -> CANCELLED	                B	      in the map and unit-tested. No cancel endpoint yet.
RUNNING -> SUCCEEDED	            B	        handler finished
RUNNING -> DEAD	                    B	    handler threw. In B this means max_attempts = 1.
RUNNING -> CANCELLED	            B        (map only)	the cooperative trigger comes in D
RUNNING -> QUEUED	                C	      lease reaper
RUNNING -> RETRYING, 
RETRYING -> RUNNING / CANCELLED	    D	        retries
DEAD -> QUEUED	                    later	    admin requeue


### Also Writes

attempts, started_at, finished_at, last_error

Arrow	                        Also writes
QUEUED -> RUNNING	            started_at, attempts + 1
RUNNING -> SUCCEEDED	        finished_at
RUNNING -> DEAD	                finished_at, last_error