(ns cuckoo.redis.jobs
  "Impure functions for manipulating the Redis jobs key."
  (:refer-clojure :exclude [key])
  (:require [cuckoo.job :as job]
            [cuckoo.redis :as redis]))

(defn key
  "Returns the redis key used to store job payloads for Cuckoo."
  [queue]
  (format "%s:jobs" (:name queue)))

(defn add
  "Adds a new job payload to the Cuckoo scheduled jobs list."
  [queue id payload]
  (= 1
     (redis/hsetnx queue
                   (key queue)
                   id
                   (job/dump-str payload))))

(defn fetch
  "Fetch a Cuckoo job by its job ID, or nil if not found."
  [queue id]
  (let [payload-str (redis/hget queue
                                (key queue)
                                id)]
    (if payload-str
      (job/load-str payload-str))))
