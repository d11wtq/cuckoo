(ns workkit.redis.jobs-test
  (:require [workkit.schedule :as schedule]
            [workkit.job :as job]
            [workkit.redis :as redis]
            [workkit.redis.jobs :as jobs])
  (:use clojure.test))

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
          (jobs/add schedule "testjob" payload)
          (is (= (job/dump-str payload)
                 (redis/hget schedule
                             (jobs/key schedule)
                             "testjob"))))

        (testing "returns true"
          (redis/flushall schedule)
          (is (true? (jobs/add schedule "testjob" payload)))))

      (testing "for a duplicate job"
        (testing "leaves the payload in redis"
          (redis/flushall schedule)
          (jobs/add schedule "testjob" payload)
          (jobs/add schedule "testjob" payload)
          (is (= (job/dump-str payload)
                 (redis/hget schedule
                             (jobs/key schedule)
                             "testjob"))))

        (testing "returns false"
          (redis/flushall schedule)
          (jobs/add schedule "testjob" payload)
          (is (false? (jobs/add schedule "testjob" payload))))))

    (testing "workkit.redis.jobs/fetch"
      (testing "for a job that exists"
        (testing "returns the job payload"
          (redis/flushall schedule)
          (redis/hset schedule
                      "test:jobs"
                      "testjob"
                      (job/dump-str payload))
          (is (= payload
                 (jobs/fetch schedule "testjob")))))

      (testing "for a job that does not exist"
        (testing "returns nil"
          (redis/flushall schedule)
          (is (nil? (jobs/fetch schedule "badjob"))))))))
