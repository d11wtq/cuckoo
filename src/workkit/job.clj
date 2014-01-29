(ns workkit.job
  "WorkKit job handling functions."
  (:require [clojure.data.json :as json]
            digest))

(defn dump-str
  "Dump a WorkKit json payload from a Clojure map."
  [payload]
  (json/write-str (assoc payload
                         :job (str (:job payload)))))

(defn parse-str
  "Parse a WorkKit json payload to a Clojure map."
  [payload-str]
  (let [payload (json/read-str payload-str :key-fn keyword)]
    (assoc payload
           :job
           (eval (read-string (:job payload))))))

(defn id
  "Computes the WorkKit job ID for the given job payload."
  [payload]
  (digest/sha1 (dump-str payload)))
