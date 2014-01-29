(ns workkit.redis.jobs-test
  (:require [workkit.schedule :as schedule]
            [workkit.job :as job]
            [workkit.redis :as redis]
            [workkit.redis.jobs :as jobs])
  (:use clojure.test))

;; (job/from-payload "")
;;
;; (job/run {:cron "", :job #'println, :args ["bob"]})

;; (jobs/get s id)
;; (jobs/all s)
;; (jobs/cancel s id)

(deftest jobs-test
  (let [schedule (schedule/create "test" {:uri (System/getenv "REDIS_URI")})
        payload {:cron "* * * * * * *" :job #'println :args ["test"]}]

    (testing "workkit.redis.jobs/key"
      (testing "uses the schedule name as a prefix"
        (redis/flushall schedule)
        (is (= "test:jobs"
               (jobs/key schedule)))))

    (testing "workkit.redis.jobs/add"
      (testing "for a non-existing job"
        (testing "adds the payload to a redis hash"
          (redis/flushall schedule)
          (jobs/add schedule "job" payload)
          (is (= (job/payload payload)
                 (redis/hget schedule
                             (jobs/key schedule)
                             "job"))))

        (testing "returns true"
          (redis/flushall schedule)
          (is (true? (jobs/add schedule "job" payload)))))

      (testing "for a duplicate job"
        (testing "leaves the payload in redis"
          (redis/flushall schedule)
          (jobs/add schedule "job" payload)
          (jobs/add schedule "job" payload)
          (is (= (job/payload payload)
                 (redis/hget schedule
                             (jobs/key schedule)
                             "job"))))

        (testing "returns false"
          (redis/flushall schedule)
          (jobs/add schedule "job" payload)
          (is (false? (jobs/add schedule "job" payload))))))))
