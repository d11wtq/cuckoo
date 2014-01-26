(ns workkit.cron
  "WorkKit cron parsing functions."
  (:require [clj-time.core :as time]
            [clj-time.local :as local-time])
  (:import java.util.Date
           org.joda.time.DateTime))

(defn parse-at
  "Parses `text`, which refers to a point in time and returns a cron line."
  [time-text]
  (let [date (time/to-time-zone
               (local-time/to-local-date-time time-text)
               (time/time-zone-for-offset 0))]
    (format "%d %d %d %s %d %d %d"
            (time/year date)
            (time/month date)
            (time/day date)
            (time/day-of-week date)
            (time/hour date)
            (time/minute date)
            (time/second date))))

(defn parse-in
  [text])

(defn parse-every
  [text])

(defn build
  "Build a WorkKit cron line for the given `time-type` and `time-text`."
  [time-type time-text]
  ((time-type
     {:at    parse-at
      :in    parse-in
      :every parse-every}) time-text))

(defn parse-cron
  "Parse the `cron-string` and return a map of the component parts."
  [cron-string]
  (zipmap [:year
           :month
           :day
           :day-of-week
           :hour
           :minute
           :second]
          (map #(Integer. %)
               (clojure.string/split cron-string #"[ \t]"))))

(defn next-time
  "Returns the next time after `date` that matches the given `cron` line."
  [cron-string java-date]
  (let [cron (parse-cron cron-string)
        date (time/to-time-zone
               (DateTime. java-date)
               (time/time-zone-for-offset 0))]
    (.toDate (time/date-time
               (or (:year cron) (time/year date))
               (or (:month cron) (time/month date))
               (or (:day cron) (time/day date))
               (or (:hour cron) (time/hour date))
               (or (:minute cron) (time/minute date))
               (or (:second cron) (time/second date))))))
