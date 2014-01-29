(ns workkit.redis.jobs-test
  (:require [workkit.schedule :as schedule]
            [workkit.job :as job]
            [workkit.redis.jobs :as jobs])
  (:use clojure.test))

;; (job/payload {:cron "", :job #'println, :args ["bob]})
;; (job/from-payload "")
;;
;; (job/id {:cron "", :job #'println, :args ["bob"]})
;;
;; (job/run {:cron "", :job #'println, :args ["bob"]})

;; (jobs/key s) "foo:jobs"
;; (jobs/add s id {:cron ... })
;; (jobs/get s id)
;; (jobs/all s)
;; (jobs/cancel s id)

(deftest jobs-test
  (let [schedule (schedule/create "test" {:uri (System/getenv "REDIS_URI")})
        payload {:cron "* * * * * * *" :job #'println :args ["test"]}]

    (testing "workkit.redis.jobs/key"
      (testing "uses the schedule name as a prefix"
        (is (= "test:jobs"
               (jobs/key schedule)))))

    (testing "workkit.redis.jobs/add"
      (testing "for a non-existing job"
        (testing "adds the payload to a redis hash"
          (jobs/add schedule "job1" payload)
          (is (= (job/payload payload)
                 (redis/hget (:conn schedule)
                             (jobs/key schedule)
                             "job1"))))

        (testing "returns true"
          (is (true? (jobs/add schedule "job2" payload))))))))
