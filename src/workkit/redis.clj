(ns workkit.redis
  "Functions for talking to Redis with some basic abstraction."
  (:require [taoensso.carmine :as carmine]))

(defmacro with-redis
  [schedule & body]
  `(carmine/wcar (:conn ~schedule)
     ~@body))

(defn flushall
  [schedule]
  (with-redis schedule
    (carmine/flushall)))

(defn hget
  [schedule key field]
  (with-redis schedule
    (carmine/hget key field)))

(defn hset
  [schedule key field value]
  (with-redis schedule
    (carmine/hset key field value)))

(defn hsetnx
  [schedule key field value]
  (with-redis schedule
    (carmine/hsetnx key field value)))
