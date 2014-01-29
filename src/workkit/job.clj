(ns workkit.job
  "WorkKit job handling functions."
  (:require [clojure.data.json :as json]
            digest))

(defn dump-str
  "Dump a WorkKit json payload from a Clojure map."
  [payload-map]
  (json/write-str (assoc payload-map
                         :job (str (:job payload-map)))))

(defn parse-str
  "Parse a WorkKit json payload to a Clojure map."
  [payload]
  (let [payload-map (json/read-str payload :key-fn keyword)]
    (assoc payload-map
           :job
           (eval (read-string (:job payload-map))))))

(defn id
  "Computes the WorkKit job ID for the given job payload."
  [payload]
  (digest/sha1 (dump-str payload)))
