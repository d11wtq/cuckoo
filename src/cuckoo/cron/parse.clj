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

(defn parse-blank
  "Parse a '?' to just return the empty set."
  [field value date]
  (if (= "?" value) #{}))

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
                  parse-blank
                  parse-wildcard
                  parse-L
                  parse-range
                  parse-integer]))))

(defn days-of-week->days-of-month
  [days-of-week date]
  (let [day1 (date/first-week-day-of-month date)]
    (reduce (fn [acc day]
              (into acc
                    (range (inc (mod (+ 7 (- day day1)) 7))
                           (inc (date/last-day-of-month date))
                           7)))
            #{}
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
  "Enable/disable one of the day of month or day of week fields, if needed.

  Cron is only complicated by the interpretation of these two fields. If both
  contain non-asterisk values, either may match (union). If only one contains
  non-asterisk values, it must match, but the other is ignored. If both fields
  are set to asterisk, all days in the month are counted.

  We do some juggling between '?' (ignored) and '*' (all values) by first
  replacing '*' with '?', then checking if both fields are ignored. If they
  are, we enable just the day of month field."
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
