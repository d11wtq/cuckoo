# WorkKit

**WARNING**: Not finished! The things in this README don't actually work; I just
like to use DDD to think things through.

A Clojure library for distributed task queueing and scheduling.

``` clojure
[workkit "0.1.0"]
```

WorkKit is a task scheduling system for Clojure applications, using Redis as
the backend. It is designed to be distributed, so that any number of machines
can participate in the management and processing of the schedule, with
guarantees that no two machines will process the same task.

Simple FIFO queuing is also possible since tasks can be schedule to happen
immediately.

Queues are processed using a thread pool and are fault tolerant.

The internal Redis data structures used by WorkKit are made public, so that
libraries can be written for interoperability with other languages, or indeed
with other Clojure libraries.

## Usage

Add the following dependency to your project.clj:

``` clojure
[workkit "0.1.0"]
```

In WorkKit, schedules (or queues, if you prefer) are given a name. You can have
multiple schedules if you need to prioritize some tasks.

### Adding to the schedule

Define a schedule, then add a task to run at a given time:

``` clojure
(ns your-app.core
  (:require [workkit.schedule :as schedule]))

(def s (schedule/create "app-tasks"
                        {:redis {:uri "redis://127.0.0.1:6379/"}}))

(schedule/add s :at "7pm tomorrow" your-app.cache/clear)
```

This will execute the function `your-app.cache/clear` at 7pm tomorrow, on any
of the servers that are listening to the "app-tasks" queue. Note that any
listening servers will need to know about `your-app.cache` in order to run this
task.

It is also possible to pass a fn as the code to be executed.

``` clojure
(schedule/add s :in "5 minutes" (fn [] (your-app.user/activate
                                         "tracy@domain.com")))
```

Be aware, however, that the fn can only include constant data. This won't work
(or rather, it will fail on some queue listener in 5 minutes):

``` clojure
(let [email "tracy@domain.com"]
  (schedule/add s :in "5 minutes" (fn [] (your-app.user/activate email))))
```

If you need to pass variable data to the queue, pass arguments explicitly:

``` clojure
(let [email "tracy@domain.com"]
  (schedule/add s :in "5 minutes"
                       #(your-app.user/activate %) [email]))
```

Arguments are encoded into the payload in Redis, so they will work fine.

Queuing at job to happen right now is just a case of scheduling it to happen at
the current time. There is a convenience for this:

``` clojure
(schedule/add-now s your-app.cache/clear)
```

### Processing the queue

Define a schedule, then listen for tasks on the queue:

``` clojure
(ns your-app.core
  (:require [workkit.schedule :as schedule]))

(def s (schedule/create "app-tasks"
                        {:redis {:uri "redis://127.0.0.1:6379/"}}))

(schedule/listen s)
```

This will poll for tasks to process and block the current thread. Tasks are
procesed in their own threads.

## Redis data structures

I publish the internal data structures used in the interest of allowing
interoperability with other platforms. While WorkKit runs Clojure code, the
internal data structures allow for theoretically any language to enqueue to and
listen to a WorkKit schedule.

There are only two keys per schedule:

  1. "S:jobs" - HASH
  2. "S:next" - ZSET (Sorted Set)

The S is replaced with the name of the schedule. For example "app-tasks:jobs".

### Jobs

This key is the primary source of truth and stores the schedule information
in JSON format. The keys in the hash are SHA1 hashes that represent job IDs.
The JSON in the values has the following structure:

``` json
{"cron":"* * * * * * *", "job":"#'some.ns/func-name", "args":[1, 2]}
```

Other fields may be added in the future. The fields of interest here are "cron"
and "job". The "job" field contains a string representation of a callable
Clojure function, be it a fn or a defn'd function. This string should be
readable by `clojure.core/read-string`. The "cron" field encodes information
about when a job should run. Unlike traditional cron, the fields are:

    *  *  *  *  *  *  *
    |  |  |  |  |  |  |
    |  |  |  |  |  |   -------- Second       (0-59)
    |  |  |  |  |   ----------- Minute       (0-59)
    |  |  |  |   -------------- Hour         (0-23)
    |  |  |   ----------------- Day of Week  (0-6) [0 = Sunday]
    |  |   -------------------- Day of Month (1-31)
    |   ----------------------- Month        (1-12)
     -------------------------- Year         (4 digits)

All times are represented as UTC. Like traditional cron, any field can be set
to a number, or a wildcard (run on every occurence).

Fields can also be set to `*/4` or `*/8`, for example, to run every 4
occurrences, or every 8 occurrences.

As expected, ranges are also supported, for example `14-16` for 2pm, 3pm, 4pm.
Likewise, comma separated values are supported, such as `14,19,22` for 2pm,
7pm, 10pm.

Unlike traditional cron, the slash syntax can be used to specify every nth
occurrence of a particular value, such as `3/2` for "every 2nd Wednesday".
This allows for some surprisingly complex intervals to be represented in a
succinct format.

In the payload shown below, "Beep!" will be printed every 5 seconds from
9am-5pm on every 2nd Sunday and every Wednesday of March 2014.

``` json
{"cron": "2014 3 * 0/2,3 9-17 * */5",
 "job":  "#'clojure.core/println",
 "args": ["Beep!"]}
```

Of course, the end user does not need to understand this format. Times are
specified using natural language processing instead. We assume that in practice
such complex intervals are not likely to be needed in a typical application.

### Next

This key is actually a queue, however it is a sorted set and not a regular list
since we need to be able to queue jobs to jump the queue, according to the time
they should run at.

The scores used to order the queue are timestamps, represented as milliseconds
since the UNIX epoch ("1970-01-01T00:00:00Z").

The values stored in the set are simply the IDs or the jobs to run, encoded in
JSON, so that additional data can be stored here if the need arises in the
future:

``` json
{"id":"abcabcabcabcabcabcabcabcabcabcabcabcabca"}
```

Values are read from this sorted set including their scores.

### Job insertion algorithm

  1. First generate the JSON payload for the job, then run a SHA1 digest on
     that string to get the job ID.
  2. Try to insert the JSON payload into the "S:jobs" hash using the job ID as
     the key, where "S" is the name of the schedule.
  3. If 0 values were added, as reported by Redis, stop processing, without
     error—this job is already in the schedule, something may currently be
     processing it.
  4. If 1 value was inserted, calculate the next time the job should run, based
     on its cron string.
  5. Convert the next run time into a 64-bit integer for the number of
     milliseconds since the UNIX epoch, and add the job ID to the "S:next"
     sorted set, using this timestamp as the score.

It is important not to process steps 4 and 5 if the job was already in "S:jobs"
as we'll see when we look at how processing works.

### Job processing algorithm

  1. First read the first value from the "S:next" queue and remove it from the
     set, atomically (use ZRANGE WITHSCORES + ZREMRANGEBYSCORE, in a MULTI
     EXEC block).
  2. Compare the score with the current time, in milliseconds since the UNIX
     epoch.
  3. If the score is later than the current time, return the job to the
     "S:next" queue, using the same score, then stop processing.
  4. If the score is earlier than the current time, read the job payload from
     "S:jobs", using the job ID.
  5. Calculate the next time the job should run, and place it back on the
     "S:next" queue with the updated timestamp, if it should run again.
  6. Evaluate the "job" and "args" fields of the payload, catching any
     exceptions that may occur.
  7. If the job does not need to run again, remove its entry from the "S:jobs"
     hash.

Because "S:next" has the queue item removed from it during processing, no two
queue listeners will process the same job. It is this removal of the job ID
from "S:next" that makes steps 3, 4, 5 in the insertion process so important.

## License

Copyright © 2014 Chris Corbyn. See the LICENSE file for details.
