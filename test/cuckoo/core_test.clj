(ns cuckoo.core-test
  (:require [cuckoo.core :as cuckoo]
            [cuckoo.job :as job]
            [cuckoo.redis.jobs :as jobs]
            [cuckoo.redis.queue :as queue]
            [cuckoo.redis :as redis])
  (:use clojure.test))

(defn read-queue
  [q]
  (take 10 (iterate (fn [_] (queue/pop* q))
                    (queue/pop* q))))

(deftest core-test
  (cuckoo/defqueue q "test" {:uri (System/getenv "REDIS_URI")})

  (testing "cuckoo.core/defqueue"
    (testing "defines a local variable from #'create-queue"
      (is (= (cuckoo/create-queue
               "test"
               {:uri (System/getenv "REDIS_URI")})
             q))))

  (testing "cuckoo.core/push*"
    (redis/flushall q)

    (let [id (cuckoo/push* q [:cron "*/4 * 6 1 ?"] '(fn [] 42))]
      (testing "returns the pushed job id"
        (is (jobs/fetch q id)))

      (testing "enqueues the job to run"
        (is (some #(= id (:id %))
                  (read-queue q)))))

    (testing "when the job is already in the schedule"
      (redis/flushall q)

      (let [payload {:cron "* * * * *" :job '(fn [] 42) :args []}
            id (job/id payload)]
        (jobs/add q id payload)

        (testing "returns the existing job id"
          (is (= id
                 (cuckoo/push* q [:cron (:cron payload)] (:job payload)))))

        (testing "does not enqueue the job"
          (cuckoo/push* q [:cron (:cron payload)] (:job payload))
          (is (not-any? #(= id (:id %))
                        (read-queue q)))))))

  (testing "cuckoo.core/push"
    (redis/flushall q)

    (let [id (cuckoo/push q [:cron "*/4 * 6 1 ?"] (fn [] 42))]
      (testing "returns the pushed job id"
        (is (jobs/fetch q id)))

      (testing "enqueues the job to run"
        (is (some #(= id (:id %))
                  (read-queue q))))

      (testing "when the job is already in the schedule"
      (redis/flushall q)

      (let [payload {:cron "* * * * *" :job '(fn [] 42) :args []}
            id (job/id payload)]
        (jobs/add q id payload)

        (testing "returns the existing job id"
          (is (= id
                 (cuckoo/push q [:cron (:cron payload)] (fn [] 42)))))

        (testing "does not enqueue the job"
          (cuckoo/push q [:cron (:cron payload)] (fn [] 42))
          (is (not-any? #(= id (:id %))
                        (read-queue q)))))))))
