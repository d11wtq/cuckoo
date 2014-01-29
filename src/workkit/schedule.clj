(ns workkit.schedule
  "WorkKit distributed Clojure task scheduler."
  (:require [taoensso.carmine :as redis])
  (:import java.util.Date))

(defn create
  "Create a new WorkKit schedule with the given name."
  ([name]
   (create name nil))
  ([name opts]
   {:name name
    :conn {:pool nil :spec opts}}))
