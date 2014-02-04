(ns cuckoo.cron.parse
  "Cuckoo cron parsing functions to convert cron strings to maps of sets."
  (:require clojure.set
            [cuckoo.date :as date])
  (:use [clojure.string :only [split]])
  (:import java.util.Date
           java.util.Calendar))

(declare parse-value)

(defn parse-integer
  "Parse a single integer string into a set."
  [field value date]
  (try
    #{(Integer. value)}
    (catch NumberFormatException e)))

;; FIXME: Clean this up
(defn parse-range
  "Parse a cron format range (with optional step) into a set."
  [field value date]
  (if (re-find #"^\d+-\d+(/\d+)?$" value)
    (let [parts (zipmap [:range :step] (split value #"/"))
          bounds (split (:range parts) #"-")]
      (set (range (Integer. (nth bounds 0))
                  (inc (Integer. (nth bounds 1)))
                  (Integer. (or (:step parts) 1)))))))

(defn parse-wildcard
  "Parse a cron format wildcard (with optional step) into a set."
  [field value date]
  (if (re-find #"\*" value)
    (parse-value
      field
      (clojure.string/replace
        value
        #"\*"
        (fn [_]
          (format "%d-%d"
                  (date/min-value field date)
                  (date/max-value field date))))
      date)))

(defn parse-list
  "Parse a comma separated list of values into a single set."
  [field value date]
  (if (re-find #"," value)
    (reduce clojure.set/union
            (map #(set (parse-value field % date))
                 (split value #",")))))

(defn parse-L
  "Parse out L in the day of month field."
  [field value date]
  (if (and (= :day field) (re-find #"L" value))
    (parse-value
      field
      (clojure.string/replace
        value
        #"L"
        (fn [_] (str (date/max-value field date))))
      date)))

(defn parse-value
  "Parse a value from a cron field into a set."
  [field value date]
  (first
    (filter (complement nil?)
            (map #(% field value date)
                 [parse-list
                  parse-L
                  parse-wildcard
                  parse-range
                  parse-integer]))))

(defn expand
  "Expand each field in `cron-map` to a set and return the new map."
  [cron-map date]
  (reduce (fn [acc [field value]]
            (merge acc {field (parse-value field value date)}))
          {} cron-map))

(defn normalize-field-count
  "Accepts 5, 6 or 7 component cron field values and normalizes to 7."
  [values]
  (let [num-values (count values)]
    (case num-values
      7 values
      6 (conj values "*")
      5 (recur (into ["0"] values))
      (throw (IllegalArgumentException.
               (format "Incorrect number of cron fields: %d"
                       num-values))))))

(defn parse
  "Parse the given cron string to a specification based on simple sets, using
  `date` as the context for L,W,* etc."
  [cron-str date]
  (expand
    (zipmap [:second
             :minute
             :hour
             :month
             :day
             :day-of-week
             :year]
            (normalize-field-count (split cron-str #"[ \t]+")))
    date))
