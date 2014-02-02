(ns cuckoo.cron
  "Cuckoo cron parsing functions."
  (:refer-clojure :exclude [seq])
  (:require [cuckoo.cron.parse :as parse]
            [cuckoo.date :as date])
  (:import java.util.Date
           java.util.Calendar))

(defn field-match?
  "Returns true if the date field value is present in the same cron set."
  [field cron-map date-map]
  ((field cron-map) (field date-map)))

(defn reset-date-fields
  "Sets all `fields` in `date-map` to their minimum possible values."
  [date-map fields]
  (reduce (fn [acc field]
            (merge acc {field (date/min-value field)}))
          date-map
          fields))

(defn advance-date
  "Returns the `date-map` with the first field of `fields` incremented by 1,
  and the less signficant fields rolled over to their base (minimum) values."
  [date-map [field & rest]]
  (let [current-value (field date-map)]
    (reset-date-fields (merge date-map {field (inc current-value)})
                       rest)))

(defn validate-or-advance
  "Helper used by `next-date*` to either return the input date, or force
  advancing to a new date and retrying, via a trampoline.

  Starts with the most significant date field and if it doesn't match the
  cron-map, increments the field, resetting the less signficant fields, before
  recuring to `next-date*`. Otherwise, if the field matches, recurs internally
  to the next most significant field.

  If all fields match, returns the input `date-map` unchanged."
  [cron-map date-map advance-fn fields]
  (if (empty? fields)
    date-map
    (let [[field & rest] fields]
      (if (field-match? field cron-map date-map)
        (recur cron-map
               date-map
               advance-fn
               rest)
        #(advance-fn
           (advance-date date-map fields))))))

(defn next-date*
  "As per `next-date`, but returns a map of date fields.

  Used by `date-map` in a trampoline recursive call."
  [cron-str date]
  (validate-or-advance (parse/parse cron-str date)
                       (date/date->map date)
                       #(next-date* cron-str (date/map->date %))
                       [:year
                        :month
                        :day
                        :hour
                        :minute
                        :second]))

(defn possible-next-match?
  "Reurn true if the given `cron-str` could possibly match a date after `date`.

  This means it must contain a date in the future from `date`."
  ([cron-str date]
   (possible-next-match? (parse/parse cron-str date)
                         (date/date->map date)
                         [:year
                          :month
                          :day
                          :hour
                          :minute
                          :second]))
  ([cron-map date-map fields]
   (or (empty? fields)
     (let [[field & rest] fields
           value (field date-map)
           allow (field cron-map)]
       (or (some #(> % value) allow)
           (and (allow value)
                (recur cron-map date-map rest)))))))

(defn next-date
  "Return the next date for which `cron-str` would match on or after `date`.

  Returns nil if there are no possible matches."
  [cron-str date]
  (if (possible-next-match? cron-str date)
    (if-let [date-map (trampoline next-date* cron-str date)]
      (date/map->date date-map))))

(defn seq
  "Return a lazy-seq of all possible dates matched by `cron-str` >= `date`."
  [cron-str date])
