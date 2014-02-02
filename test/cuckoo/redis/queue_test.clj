(ns cuckoo.redis.queue-test
  (:require [cuckoo.schedule :as schedule]
            [cuckoo.redis :as redis]
            [cuckoo.redis.queue :as queue]
            [clojure.data.json :as json])
  (:use clojure.test)
  (:import java.util.Date))

(deftest queue-test
  (let [schedule (schedule/create "test" {:uri (System/getenv "REDIS_URI")})]

    (testing "cuckoo.redis.queue/key"
      (testing "uses the schedule name as a prefix"
        (is (= "test:next"
               (queue/key schedule)))))

    (testing "cuckoo.redis.queue/add"
      (testing "adds id to a sorted set using date as score"
        (redis/flushall schedule)
        (queue/add schedule {:id "test" :date (Date. (- 2014 1900) 0 1)})
        (is (= ["{\"id\":\"test\"}" "1388534400000"]
               (redis/zrange schedule
                             (queue/key schedule)
                             0 1
                             :withscores)))))

    (testing "cuckoo.redis.queue/pop"
      (letfn [(install-jobs [job-offsets]
                (redis/flushall schedule)
                (doseq [[id offset] job-offsets]
                  (redis/zadd schedule
                              (queue/key schedule)
                              (+ offset (.getTime (Date.)))
                              (json/write-str {:id id}))))]

        (testing "when there are jobs due to run"
          (install-jobs {"job1" 60000 "job2" -30 "job3" 90000})

          (let [job (queue/pop schedule)]
            (testing "returns the job with the lowest score"
              (is (= "job2" (:id job))))

            (testing "includes the run date"
              (is (<= (.getTime (:date job))
                      (- (.getTime (Date.)) 30))))

            (testing "removes the first job from the queue"
              (is (= "{\"id\":\"job1\"}"
                     (first
                       (redis/zrange schedule
                                     (queue/key schedule)
                                     0 0
                                     :withscores)))))))

        (testing "when there are no jobs yet due to run"
          (install-jobs {"job1" 80000 "job2" 90000 "job3" 60000})

          (let [job (queue/pop schedule)]
            (testing "returns nil"
              (is (nil? job)))

            (testing "does not modify the queue"
              (is (= "{\"id\":\"job3\"}"
                     (first
                       (redis/zrange schedule
                                     (queue/key schedule)
                                     0 0
                                     :withscores)))))))

        (testing "when the queue is empty"
          (testing "returns nil"
            (install-jobs {})
            (is (nil? (queue/pop schedule)))))))))
