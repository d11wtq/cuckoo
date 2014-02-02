(ns cuckoo.redis.jobs
  "Impure functions for manipulating the Redis jobs key."
  (:refer-clojure :exclude [key])
  (:require [cuckoo.job :as job]
            [cuckoo.redis :as redis]))

(defn key
  "Returns the redis key used to store job payloads for Cuckoo."
  [schedule]
  (format "%s:jobs" (:name schedule)))

(defn add
  "Adds a new job payload to the Cuckoo scheduled jobs list."
  [schedule id payload]
  (= 1
     (redis/hsetnx schedule
                   (key schedule)
                   id
                   (job/dump-str payload))))

(defn fetch
  "Fetch a Cuckoo job by its job ID, or nil if not found."
  [schedule id]
  (let [payload-str (redis/hget schedule
                                (key schedule)
                                id)]
    (if payload-str
      (job/load-str payload-str))))
