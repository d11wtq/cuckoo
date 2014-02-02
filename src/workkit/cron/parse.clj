(ns workkit.cron.parse
  "WorkKit cron parsing functions."
  (:require clojure.set)
  (:use [clojure.string :only [split]])
  (:import java.util.Date
           java.util.Calendar))

(defn last-day-of-month
  [date]
  (let [c (Calendar/getInstance)]
    (.setTime c date)
    (.getActualMaximum c Calendar/DAY_OF_MONTH)))

(defn min-value
  [field date]
  (case field
    :second      0
    :minute      0
    :hour        0
    :month       1
    :day         1
    :day-of-week 0
    :year        1970))

(defn max-value
  [field date]
  (case field
    :second      59
    :minute      59
    :hour        23
    :month       12
    :day         (last-day-of-month date)
    :day-of-week 6
    :year        2099))

(defn parse-integer
  [field value date]
  (try
    #{(Integer. value)}
    (catch NumberFormatException e)))

(defn parse-range
  [field value date]
  (if (re-find #"^\d+-\d+(/\d+)?$" value)
    (let [parts (zipmap [:range :step] (split value #"/"))
          bounds (split (:range parts) #"-")]
      (range (Integer. (nth bounds 0))
             (inc (Integer. (nth bounds 1)))
             (Integer. (or (:step parts) 1))))))

(defn parse-wildcard
  [field value date]
  (parse-range
    field
    (clojure.string/replace
      value
      #"\*"
      (fn [_]
        (format "%d-%d"
                (min-value field date)
                (max-value field date))))
    date))

(defn parse-value
  [field value date]
  (first
    (filter (complement nil?)
            (map #(% field value date)
                 [parse-wildcard
                  parse-range
                  parse-integer]))))

(defn parse-value-list
  [field value-list date]
  (reduce clojure.set/union
          (map #(set (parse-value field % date))
               (split value-list #","))))

(defn parse-fields
  [cron-map date]
  (reduce (fn [acc [field value]]
            (assoc acc
                   field
                   (parse-value-list field value date)))
          {}
          cron-map))

(defn parse
  "Parse the given cron string to a specification based on simple sets, using
  `date` as the context for L,W,* etc."
  [cron-str date]
  (parse-fields
    (zipmap [:second
             :minute
             :hour
             :month
             :day
             :day-of-week
             :year]
            (split cron-str #"[ \t]+"))
    date))
