(ns workkit.redis.queue-test
  (:require [workkit.schedule :as schedule]
            [workkit.redis :as redis]
            [workkit.redis.queue :as queue])
  (:use clojure.test)
  (:import java.util.Date))

(deftest queue-test
  (let [schedule (schedule/create "test" {:uri (System/getenv "REDIS_URI")})]

    (testing "workkit.redis.queue/key"
      (testing "uses the schedule name as a prefix"
        (is (= "test:next"
               (queue/key schedule)))))

    (testing "workkit.redis.queue/add"
      (testing "adds id to a sorted set using date as score"
        (redis/flushall schedule)
        (queue/add schedule {:id "test" :date (Date. (- 2014 1900) 0 1)})
        (is (= ["{\"id\":\"test\"}" "1388534400000"]
               (redis/zrange schedule
                             (queue/key schedule)
                             0 1
                             :withscores)))))))
