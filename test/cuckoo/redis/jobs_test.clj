(ns cuckoo.redis.jobs-test
  (:require [cuckoo.core :as cuckoo]
            [cuckoo.job :as job]
            [cuckoo.redis :as redis]
            [cuckoo.redis.jobs :as jobs])
  (:use clojure.test))


(deftest jobs-test
  (cuckoo/defqueue q "test" {:uri (System/getenv "REDIS_URI")})

  (let [payload {:cron "* * * * * * *" :job #'println :args ["test"]}]

    (testing "cuckoo.redis.jobs/key"
      (testing "uses the queue name as a prefix"
        (redis/flushall q)
        (is (= "test:jobs"
               (jobs/key q)))))

    (testing "cuckoo.redis.jobs/add"
      (testing "for a non-existing job"
        (testing "adds the payload to a redis hash"
          (redis/flushall q)
          (jobs/add q "testjob" payload)
          (is (= (job/dump-str payload)
                 (redis/hget q
                             (jobs/key q)
                             "testjob"))))

        (testing "returns true"
          (redis/flushall q)
          (is (true? (jobs/add q "testjob" payload)))))

      (testing "for a duplicate job"
        (testing "leaves the payload in redis"
          (redis/flushall q)
          (jobs/add q "testjob" payload)
          (jobs/add q "testjob" payload)
          (is (= (job/dump-str payload)
                 (redis/hget q
                             (jobs/key q)
                             "testjob"))))

        (testing "returns false"
          (redis/flushall q)
          (jobs/add q "testjob" payload)
          (is (false? (jobs/add q "testjob" payload))))))

    (testing "cuckoo.redis.jobs/fetch"
      (testing "for a job that exists"
        (testing "returns the job payload"
          (redis/flushall q)
          (redis/hset q
                      "test:jobs"
                      "testjob"
                      (job/dump-str payload))
          (is (= payload
                 (jobs/fetch q "testjob")))))

      (testing "for a job that does not exist"
        (testing "returns nil"
          (redis/flushall q)
          (is (nil? (jobs/fetch q "badjob"))))))))
