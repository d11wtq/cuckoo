(ns workkit.redis.queue
  "Impure functions for manipulating the Redis queue key."
  (:refer-clojure :exclude [key pop])
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

(defn pop*
  "Take the first job from the WorkKit queue and return its ID & run date."
  [schedule]
  (let [[[queue-item-str score]] (redis/atomic schedule
                                   (redis/zrange (key schedule)
                                                 0 0
                                                 :withscores)
                                   (redis/zremrangebyrank (key schedule)
                                                          0 0))]
    (if (and queue-item-str score)
      (assoc (queue-item/load-str queue-item-str)
             :date (queue-item/load-score score)))))

(defn pop
  "Take the first job from the WorkKit queue and return its ID and run date
  only if it is due to run. Return nil in all other cases."
  [schedule]
  (let [queue-item (pop* schedule)]
    (if queue-item
      (if (queue-item/before? queue-item (java.util.Date.))
        queue-item
        (and (add schedule queue-item) nil)))))
