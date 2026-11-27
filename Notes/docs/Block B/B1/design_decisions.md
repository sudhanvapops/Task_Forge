Two consequences:

The enum can keep all six values, and the CHECK constraint in B2 can list all six. That avoids a migration later.
In B3, the arrows tagged C and D are illegal for now, so your tests assert them as illegal. When Block C or D arrives, you flip the arrow in the map and flip its test. The tests then force you to notice the change.

