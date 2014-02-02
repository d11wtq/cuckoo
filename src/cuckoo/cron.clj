(ns cuckoo.cron
  "Cuckoo cron parsing functions."
  (:refer-clojure :exclude [seq])
  (:use [cuckoo.cron.parse :only [parse]]
        [clojure.string :only [split]])
  (:import java.util.Date))

(defn next-date
  [cron-str date])

(defn seq
  "Return a lazy-seq of all possible dates matched by `cron-str` >= `date`."
  [cron-str date])
