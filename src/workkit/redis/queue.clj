(ns workkit.redis.queue
  "Impure functions for manipulating the Redis queue key."
  (:refer-clojure :exclude [key])
  (:require [workkit.queue-item :as queue-item]
            [workkit.redis :as redis]))

(defn key
  "Returns the name of the redis key for the WorkKit schedule queue."
  [schedule]
  (format "%s:next" (:name schedule)))

(defn add
  "Insert a job id into the schedule to run at a given date."
  [schedule payload]
  (redis/zadd schedule
              (key schedule)
              (queue-item/dump-score (:date payload))
              (queue-item/dump-str payload)))

;(defn pop*
;  [schedule]
;  (let [[queue-item-str score] (redis/multi schedule
;                                 (redis/zrange (key schedule)
;                                               0 1
;                                               :withscores)
;                                 (redis/zremrangebyrank (key schedule)
;                                                        0 0))]
;    (assoc (queue-item/load-str queue-item-str)
;           :date (queue-item/load-score score))))
;
;(defn pop
;  [schedule]
;  (let [queue-item (pop* schedule)]
;    (if (queue-item/before? queue-item (Date.))
;      queue-item
;      (and (add schedule queue-item) nil))))
