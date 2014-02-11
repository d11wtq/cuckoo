(ns cuckoo.redis.queue
  "Impure functions for manipulating the Redis queue key."
  (:refer-clojure :exclude [key pop])
  (:require [cuckoo.queue-item :as queue-item]
            [cuckoo.redis :as redis]))

(defn key
  "Returns the name of the redis key for the Cuckoo schedule queue."
  [queue]
  (format "%s:next" (:name queue)))

(defn add
  "Insert a job id into the queue to run at a given date."
  [queue payload]
  (redis/zadd queue
              (key queue)
              (queue-item/dump-score (:date payload))
              (queue-item/dump-str payload)))

(defn pop*
  "Take the first job from the Cuckoo queue and return its ID & run date."
  [queue]
  (let [[[queue-item-str score]] (redis/atomic queue
                                   (redis/zrange (key queue)
                                                 0 0
                                                 :withscores)
                                   (redis/zremrangebyrank (key queue)
                                                          0 0))]
    (if (and queue-item-str score)
      (assoc (queue-item/load-str queue-item-str)
             :date (queue-item/load-score score)))))

(defn pop
  "Take the first job from the Cuckoo queue and return its ID and run date
  only if it is due to run. Return nil in all other cases."
  [queue]
  (if-let [queue-item (pop* queue)]
    (if (queue-item/before? queue-item (java.util.Date.))
      queue-item
      (and (add queue queue-item) nil))))
