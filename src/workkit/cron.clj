(ns workkit.cron
  "WorkKit cron parsing functions."
  (:require [clj-time.core :as time]
            [clj-time.local :as local-time])
  (:use [clojure.string :only [split]])
  (:import java.util.Date
           org.joda.time.DateTime))


(defn add-default-values
  [values]
  (case (count values)
    5 (into ["0"] (conj (vec values) "*"))
    7 values
    nil))

(defn parse-numeric
  [value]
  (vec (map #(Integer. %) (split value #","))))

(defn parse-values
  [values]
  (if-let [values (add-default-values values)]
    (let [parse-fns {:second      parse-numeric
                     :minute      identity
                     :hour        identity
                     :month       identity
                     :day         identity
                     :day-of-week identity
                     :year        identity}]
      (apply hash-map
             (flatten
               (map (fn [value [field parse-fn]]
                      [field (parse-fn value)])
                    values
                    parse-fns))))))

(defn load-str
  "Parse a cron string into a structured map of its component parts."
  [cron-str]
  (or (parse-values (split cron-str #"[ \t]+"))
      (throw (IllegalArgumentException.
               (format "Invalid cron string: %s" cron-str)))))
