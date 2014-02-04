(ns cuckoo.date
  "Date abstraction functions for Cuckoo."
  (:import java.util.Date
           java.util.Calendar))

(def fields
  "Lookup table for transforming between java.util.Date and an internal map."
  [[:year        Calendar/YEAR          0]
   [:month       Calendar/MONTH         1]
   [:day         Calendar/DAY_OF_MONTH  0]
   [:hour        Calendar/HOUR_OF_DAY   0]
   [:minute      Calendar/MINUTE        0]
   [:second      Calendar/SECOND        0]])

(defn date->map
  "Converts a java.util.Date into a Clojure map of fields."
  [date]
  (let [calendar (doto (Calendar/getInstance)
                   (.setTime date))]
    (reduce (fn [acc [field java-field offset]]
              (merge acc {field (+ (.get calendar java-field) offset)}))
            {} fields)))

(defn map->date
  "Converts a Clojure map of fields to a java.util.Date."
  [map]
  (let [calendar (doto (Calendar/getInstance)
                   (.set Calendar/MILLISECOND 0))]
    (doseq [[field java-field offset] fields]
      (.set calendar java-field (- (field map) offset)))
    (.getTime calendar)))

(defn first-week-day-of-month
  "Get the day of week at the start of this month."
  [date]
  (dec
    (.get
      (doto (Calendar/getInstance)
        (.setTime date)
        (.set Calendar/DAY_OF_MONTH 1))
      Calendar/DAY_OF_WEEK)))

(defn last-day-of-month
  "Returns the last possible day of the month given by `date`."
  [date]
  (.getActualMaximum
    (doto (Calendar/getInstance)
      (.setTime date))
    Calendar/DAY_OF_MONTH))

(defn max-value
  "Returns the maximum value for cron `field` in the context of `date`."
  [field date]
  (case field
    :second      59
    :minute      59
    :hour        23
    :month       12
    :day         (last-day-of-month date)
    :day-of-week 6
    :year        2099))

(defn min-value
  "Returns the minimum value for the date `field` in the context of `date`."
  ([field date]
   (min-value field))
  ([field]
   (case field
     :second      0
     :minute      0
     :hour        0
     :month       1
     :day         1
     :day-of-week 0
     :year        1970)))
