(ns workkit.redis
  "Functions for talking to Redis with some basic abstraction."
  (:require [taoensso.carmine :as carmine]))

(defmacro with-redis
  [schedule & body]
  `(carmine/wcar (:conn ~schedule)
     ~@body))

(defn hget
  [schedule key field]
  (with-redis schedule
    (carmine/hget key field)))

(defn hsetnx
  [schedule key field value]
  (with-redis schedule
    (carmine/hsetnx key field value)))
