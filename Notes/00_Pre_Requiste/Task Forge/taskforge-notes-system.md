# TaskForge Phase 1 — Notes System

Where to write what you learned, which problems you hit, which you solved, and **how you isolated each one**. Works alongside the mentor briefs; nothing in those needs rebuilding.

**The one rule:** you write the notes, in your own words. The LLM may review them, never write them for you. A note you didn't write teaches you nothing and is useless in an interview.

---

## 1. Files (create all in Block A, task A0)

```text
taskforge/docs/
├── learning-log.md    what I learned: micro notes inside tasks + one wrap-up per block
├── error-log.md       every problem: symptom -> how I isolated it -> root cause -> fix
├── tradeoffs.md       decisions: options, choice, why, what I'd do differently
├── state-machine.md   diagram + legal AND illegal transitions (update on every change)
├── chaos-log.md       every failure I injected and what happened
├── parking-lot.md     ideas I'm NOT doing now
├── skill-log.md       hint rungs used, closed-book results, weak spots
└── daily-log.md       (optional) 3 lines per day, kills "where was I?"
```

---

## 2. When to write (three moments, so it never feels like homework)

| Moment | What | Size | Where |
|---|---|---|---|
| **During** a task | One-line scratch notes: "tried X -> saw Y" | seconds | top of today's `daily-log.md` |
| **After each micro-task** | Learning note + any error entry | 3-5 lines | `learning-log.md`, `error-log.md` |
| **End of each block** | Block wrap-up + closed-book results | 10-15 lines | `learning-log.md`, `skill-log.md` |

Write inside a section as many times as you like. Many small notes beat one big write-up you postpone.

---

## 3. Templates (copy, paste, fill)

### 3.1 Micro learning note (after each micro-task)

```text
## [C2] SKIP LOCKED claim  ·  2026-11-12
Before I thought:
Now I know:
Proof (command / output / test name):
Source I used (doc name + section):
Still fuzzy:
```

### 3.2 Scratch line (during a task)

```text
14:20  tried: <what>   saw: <what happened>   next: <idea>
```

### 3.3 Error-log entry (every problem, solved or not)

```text
## E-014 · [C1] Same job executed twice          Status: SOLVED / OPEN
Date: 2026-11-12   Time lost: 1h40   Hint rung used: L2
SYMPTOM (exact error, output or numbers):
EXPECTED vs ACTUAL:
HOW I ISOLATED IT:
  1. Reproduced reliably by:
  2. Shrunk it to the smallest case:
  3. Hypotheses (list all, tick the ones ruled out and how):
       [ ] H1 ...  ruled out by: ...
       [ ] H2 ...
  4. Experiment that proved the cause:
ROOT CAUSE (one sentence):
FIX:
WHY IT WASN'T OBVIOUS:
TEST / GUARD ADDED SO IT CAN'T COME BACK:
60-SECOND INTERVIEW VERSION:
```

**Isolation toolbox** (pick from this list when writing step 2-4):
reproduce on demand · remove things until it stops failing · change ONE thing at a time · binary-search the code/commits · turn on SQL logging · run the SQL by hand in psql · two psql sessions for locking · compare a working run with a broken run · add a log line with thread/worker id · write the failing test first.

### 3.4 Open-problem line (unsolved things you want to come back to)

```text
Q-007 · [D6] Why do in-flight jobs lose their lease during shutdown?   Tried: ...   Next experiment: ...
```
When solved, turn it into an `error-log.md` entry.

### 3.5 Tradeoff entry

```text
## T-006 · RETRYING as a separate state vs QUEUED + future next_run_at
Options: A) ...  B) ...
Chose: ...
Why:
What it costs me:
What I'd do differently / revisit when:
```

### 3.6 Parking-lot entry

```text
2026-11-12 · LISTEN/NOTIFY instead of polling · tempting because: lower latency · parked because: not needed for exit criteria · revisit: after Phase 1 / Phase 4
```
Rule: write it down, close the tab, go back to the task.

### 3.7 Chaos-log row

```text
| # | Failure injected | How | Predicted | Actual | Fixed? | Test added |
|---|---|---|---|---|---|---|
| 1 | kill -9 mid-job | kill -9 <pid> | stuck RUNNING | reclaimed after lease | yes (C5) | LeaseReclaimTest |
```

### 3.8 Skill-log line (per task, takes 20 seconds)

```text
[C6] rung used: L2 · minutes stuck: 70 · what unlocked it: SIGSTOP experiment showed old worker still writing
Closed-book (Block C): claim SQL = SOLID · fencing = SHAKY · lease tradeoff = SOLID  -> revisit fencing on 2026-11-20
```

### 3.9 Block wrap-up (end of every block)

```text
# BLOCK X WRAP-UP · started ___ · done ___ · hours ___
I can now (without help):
Still shaky:
Top 3 problems I hit (link E-ids):
One thing that surprised me:
What I'd do differently:
One-paragraph explanation of this block for a friend, no jargon:
```

### 3.10 Daily log (optional)

```text
2026-11-12 · Block C
Did:
Stuck on:
Next (ONE micro-task):
```

---

## 4. What to capture in each block (prompts, not answers)

| Block | Make sure your notes include |
|---|---|
| **A** | Your own heap numbers from the unbounded-queue drill; the thread-name evidence for which executor Spring really used; why 202 |
| **B** | The stuck-RUNNING reproduction (what was lost, where it lived); your list of illegal transitions and what each would break; the SQL Hibernate emitted for `@Version` |
| **C** | The double-execution timeline you drew; pool-exhaustion observations with timings; what the zombie did on resume BEFORE you built the guard; claim SQL written from memory (date it, compare later) |
| **D** | Both jitter plots; your RETRYING decision and reasoning; shutdown order of operations; which exceptions you classified retryable vs permanent |
| **E** | The interleaving that breaks check-then-insert; duplicate counts before/after the constraint; your own words on why exactly-once delivery is impossible |
| **F** | Cancel-vs-claim race results (1000 runs); what the API promised vs delivered for a handler that ignores cancel; EXPLAIN ANALYZE before/after |
| **G** | Throughput and p50/p95/p99 numbers; the single metric you'd watch and why; Postgres-restart observations; the final chaos table |

---

## 5. Weekly 10-minute review (spaced retrieval)

1. Open `error-log.md`. Pick **3 old entries** at random.
2. Cover the ROOT CAUSE and FIX. Re-explain each from the symptom alone, out loud.
3. Anything you can't explain goes to `skill-log.md` as SHAKY with a revisit date.
4. Re-read one `tradeoffs.md` entry and ask: would I decide the same today?

---

## 6. Add-on for the mentor briefs (paste this under any block brief)

```text
NOTES RULE (add-on)
After each micro-task, ask me to write my learning note in my own words (what I thought before, what I know now, proof, source). Review it: point out vague wording, missing evidence, or anything wrong, as questions first. Never write the note for me.
When I hit a problem, ask me to fill the isolation steps of my error-log entry (reproduce, shrink, hypotheses, experiment) BEFORE giving any hint rung above L1. If I can't list hypotheses, that is the first thing to practise.
At block end: ask for my wrap-up, my skill-log lines, and my 60-second interview version of my top 3 bugs. Give feedback on clarity and correctness.
If I paste raw scratch notes, help me organise them only after I have written a first draft myself.
```

---

## 7. Why this matters beyond learning

- `error-log.md` + `chaos-log.md` become your interview stories ("symptom, how I isolated it, root cause, test I added").
- `tradeoffs.md` feeds the README's "what I'd do differently" section.
- `learning-log.md` and the block wrap-ups give you raw material for the one post per project.
