(ns workkit.redis.jobs
  "Impure functions for manipulating the Redis jobs key."
  (:refer-clojure :exclude [key])
  (:require [workkit.job :as job]
            [workkit.redis :as redis]))

(defn key
  "Returns the redis key used to store job payloads for WorkKit."
  [schedule]
  (format "%s:jobs" (:name schedule)))

(defn add
  "Adds a new job payload to the WorkKit scheduled jobs list."
  [schedule id payload]
  (= 1
     (redis/hsetnx schedule
                   (key schedule)
                   id
                   (job/payload payload))))