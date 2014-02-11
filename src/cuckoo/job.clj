(ns cuckoo.job
  "Cuckoo job handling functions."
  (:require [clojure.data.json :as json]
            digest))

(defn dump-str
  "Dump a Cuckoo json payload from a Clojure map."
  [payload]
  (json/write-str (into (sorted-map)
                        (assoc payload
                               :job (str (:job payload))
                               :args (vec (:args payload))))))

(defn load-str
  "Parse a Cuckoo json payload to a Clojure map."
  [payload-str]
  (let [payload (json/read-str payload-str :key-fn keyword)]
    (assoc payload
           :job
           (eval (read-string (:job payload))))))

(defn id
  "Computes the Cuckoo job ID for the given job payload."
  [payload]
  (digest/sha1 (dump-str payload)))

(defn run
  "Runs the job in Cuckoo payload by applying its fn to its args."
  [payload]
  (apply (:job payload) (:args payload)))
