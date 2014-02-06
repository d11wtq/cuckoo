(ns cuckoo.cron.parse
  "Cuckoo cron parsing functions to convert cron strings to maps of sets."
  (:require [cuckoo.date :as date])
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

(defn parse-range
  "Parse a cron format range (with optional step) into a set."
  [field value date]
  (let [groups (re-seq #"^(\d+)-(\d+)(/(\d+))?$" value)]
    (if (not (empty? groups))
      (reduce (fn [acc [_ start end _ step]]
                (into acc (range (Integer. start)
                                 (inc (Integer. end))
                                 (Integer. (or step 1)))))
              (set [])
              groups))))

(defn parse-?
  "Parse a '?' to just return the empty set."
  [field value date]
  (if (= "?" value) #{}))

(defn parse-*
  "Parse '*' (with optional step) into a set."
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
    (reduce into (map #(set (parse-value field % date))
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

(defn short-name-matcher
  "Returns a regex that matches the first 3 chars of a long string."
  [value]
  (re-pattern (format "(?i)%s(%s)?"
                      (subs value 0 3)
                      (subs value 3))))

(defn parse-names
  "Parse named values into integers, using a lookup table."
  [field value date mapping]
  (if (some #(re-find % value) (map short-name-matcher (keys mapping)))
    (parse-value
      field
      (reduce (fn [acc [long-name intval]]
                (clojure.string/replace
                  acc
                  (short-name-matcher long-name)
                  (str intval)))
              value
              mapping)
      date)))

(defn parse-day-names
  "Parse named days instead of integers."
  [field value date]
  (if (= :day-of-week field)
    (parse-names field value date {"SUNDAY"    0
                                   "MONDAY"    1
                                   "TUESDAY"   2
                                   "WEDNESDAY" 3
                                   "THURSDAY"  4
                                   "FRIDAY"    5
                                   "SATURDAY"  6})))

(defn parse-month-names
  "Parse named months instead of integers."
  [field value date]
  (if (= :month field)
    (parse-names field value date {"JANUARY"   1
                                   "FEBRUARY"  2
                                   "MARCH"     3
                                   "APRIL"     4
                                   "MAY"       5
                                   "JUNE"      6
                                   "JULY"      7
                                   "AUGUST"    8
                                   "SEPTEMBER" 9
                                   "OCTOBER"   10
                                   "NOVEMBER"  11
                                   "DECEMBER"  12})))

(defn parse-value
  "Parse a value from a cron field into a set."
  [field value date]
  (first
    (filter (complement nil?)
            (map #(% field value date)
                 [parse-list
                  parse-?
                  parse-*
                  parse-L
                  parse-day-names
                  parse-month-names
                  parse-range
                  parse-integer]))))

(defn days-of-week->days-of-month
  "Converts a set of days in a week into the same days in the month."
  [days-of-week date]
  (let [day1 (date/first-week-day-of-month date)]
    (reduce (fn [acc day]
              (into acc
                    (range (inc (mod (+ 7 (- day day1)) 7))
                           (inc (date/last-day-of-month date))
                           7)))
            (set [])
            days-of-week)))

(defn normalize-wildcards
  "Translates '*' to '?' on the cron date fields."
  [cron-map]
  (reduce (fn [acc k]
            (if (= "*" (k acc))
              (merge acc {k "?"})
              acc))
          cron-map
          [:day :day-of-week]))

(defn normalize-day-fields
  "Make sure at most one of :day-of-week and :day has a '?' symbol."
  [cron-map]
  (let [normalized-map (normalize-wildcards cron-map)]
    (if (and (= "?" (:day-of-week normalized-map))
             (= "?" (:day normalized-map)))
      (merge normalized-map {:day "*"})
      normalized-map)))

(defn expand-raw
  "Literally expand each field into the basic numeric values.

  This basically does not consider the union between :day and :day-of-week."
  [cron-map date]
  (reduce (fn [acc [field value]]
            (merge acc
                   {field (parse-value field value date)}))
          {}
          (normalize-day-fields cron-map)))

(defn expand
  "Expand each field in `cron-map` to a set and return the new map."
  [cron-map date]
  (let [raw-values (expand-raw cron-map date)]
    (dissoc
      (merge raw-values
             {:day (into (:day raw-values)
                         (days-of-week->days-of-month
                           (:day-of-week raw-values)
                           date))})
      :day-of-week)))

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
