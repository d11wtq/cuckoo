(ns workkit.queue-item
  "WorkKit queued job handling functions."
  (:require [clojure.data.json :as json]))

(defn dump-str
  "Dump a WorkKit queued job json payload from a Clojure map."
  [payload]
  (json/write-str (dissoc payload :date)))

(defn load-str
  "Parse a WorkKit queued job json payload to a Clojure map."
  [payload-str]
  (json/read-str payload-str :key-fn keyword))

(defn dump-score
  "Convert a java.util.Date to an integer formatted as a string."
  [date]
  (str (clj-time.coerce/to-long (clj-time.coerce/from-date date))))

(defn load-score
  "Create a java.util.Date instance from an integer formatted as a string."
  [integer]
  (.toDate (clj-time.coerce/from-long (Long. integer))))

(defn before?
  [payload date]
  (.before (:date payload) date))
