(ns workkit.cron
  "WorkKit cron parsing functions."
  (:refer-clojure :exclude [seq])
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

(defn parse-numeric-range
  [value]
  (case value
    "*" \*
    "?" \*
    (Integer. value)))

(defn parse-numeric-list
  [value]
  (vec (map parse-numeric-range (split value #","))))

(defn parse-values
  [values]
  (if-let [values (add-default-values values)]
    (let [parse-fns {:second      parse-numeric-list
                     :minute      parse-numeric-list
                     :hour        parse-numeric-list
                     :month       parse-numeric-list
                     :day         parse-numeric-list
                     :day-of-week parse-numeric-list
                     :year        parse-numeric-list}]
      (reduce merge (map (fn [value [field parse-fn]]
                           {field (parse-fn value)})
                         values
                         parse-fns)))))

(defn load-str
  "Parse a cron string into a structured map of its component parts."
  [cron-str]
  (or (parse-values (split cron-str #"[ \t]+"))
      (throw (IllegalArgumentException.
               (format "Invalid cron string: %s" cron-str)))))

(defn seq
  "Return a lazy-seq of all possible dates matched by `cron-str` > `date`."
  [cron-str date]
  (take-while
    (complement nil?)
    (iterate (fn [d] (Date. (+ 1000 (.getTime d)))) date)))
