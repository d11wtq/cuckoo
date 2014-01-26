(ns workkit.job
  "WorkKit job handling functions."
  (:require [clojure.data.json :as json]
            digest))

(defn payload
  "Builds a JSON payload for this job."
  [cron-string job args]
  (json/write-str {:cron cron-string
                   :job  (str job)
                   :args args}))

(defn id
  "Computes a unique ID for this job."
  [cron-string job args]
  (digest/sha1 (payload cron-string job args)))
