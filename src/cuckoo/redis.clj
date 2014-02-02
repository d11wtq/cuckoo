(ns cuckoo.redis
  "Functions for talking to Redis with some basic abstraction."
  (:require [taoensso.carmine :as carmine]))

(defmacro with-redis
  [schedule & body]
  `(carmine/wcar (:conn ~schedule)
     ~@body))

(defn flushall
  ([]
   (carmine/flushall))
  ([schedule]
   (with-redis schedule
     (flushall))))

(defn hget
  ([key field]
   (carmine/hget key field))
  ([schedule key field]
   (with-redis schedule
     (hget key field))))

(defn hset
  ([key field value]
   (carmine/hset key field value))
  ([schedule key field value]
   (with-redis schedule
     (hset key field value))))

(defn hsetnx
  ([key field value]
   (carmine/hsetnx key field value))
  ([schedule key field value]
   (with-redis schedule
     (hsetnx key field value))))

(defn zadd
  ([key score value]
   (carmine/zadd key score value))
  ([schedule key score value]
   (with-redis schedule
     (zadd key score value))))

(defn zrange
  ([key start stop opts]
   (carmine/zrange key start stop opts))
  ([schedule key start stop opts]
   (with-redis schedule
     (zrange key start stop opts))))

(defn zremrangebyrank
  ([key start stop]
   (carmine/zremrangebyrank key start stop))
  ([schedule key start stop]
   (with-redis schedule
     (zremrangebyrank key start stop))))

(defn multi
  ([]
   (carmine/multi))
  ([schedule]
   (with-redis schedule
     (multi))))

(defn exec
  ([]
   (carmine/exec))
  ([schedule]
   (with-redis schedule
     (exec))))

(defmacro atomic
  [schedule & body]
  `(last
     (with-redis ~schedule
       (multi)
       ~@body
       (exec))))
