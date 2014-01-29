(ns workkit.job
  "WorkKit job handling functions."
  (:require [clojure.data.json :as json]
            digest))

(defn payload
  "Builds a json payload for this WorkKit job."
  [payload-map]
  (json/write-str payload-map))

(defn id
  "Computes a unique ID for this WorkKit job."
  [payload-map]
  (digest/sha1 (payload payload-map)))
