(ns cuckoo.core
  "Cuckoo — Distributed queueing & scheduling functions."
  (:require [cuckoo.job :as job]
            [cuckoo.queue-item :as item]
            [cuckoo.redis.jobs :as jobs]
            [cuckoo.redis.queue :as queue]
            [cuckoo.cron :as cron])
  (:import java.util.Date))

(defmacro defqueue
  "Create and define a new Cuckoo queue variable."
  [var-name & args]
  `(def ~var-name
     (create-queue ~@args)))

(defn create-queue
  "Create a new Cuckoo queue with the given name."
  ([name]
   (create-queue name nil))
  ([name opts]
   {:name name
    :conn {:pool nil :spec opts}}))

(defn portable-fn
  "Accepts a fn list and returns an equivalent, but more portable version."
  [fn-data]
  (let [wrapper (if (symbol? fn-data)
                  `resolve
                  `quote)]
    (list wrapper fn-data)))

(defn run-time->cron-str
  "Convert a Cuckoo run time vector or keyword to a cron string."
  [run-time]
  (last run-time))

(defmacro push
  "Add a job to a Cuckoo queue to be performed remotely."
  [queue run-time fn-data & args]
  `(push* ~queue
          ~run-time
          ~(portable-fn fn-data)
          ~@args))

(defn push*
  "The same as macro cuckoo.core/push, except requires `fn-data` to be quoted."
  [queue run-time fn-data & args]
  (let [payload {:cron (run-time->cron-str run-time)
                 :job fn-data
                 :args args}
        job-id (job/id payload)]
    (when (jobs/add queue job-id payload)
      (queue/add queue
                 {:id job-id
                  :date (cron/next-date
                          (:cron payload)
                          (Date.))}))
    job-id))
