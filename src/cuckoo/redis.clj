(ns cuckoo.redis
  "Functions for talking to Redis with some basic abstraction."
  (:require [taoensso.carmine :as carmine]))

(defmacro with-redis
  [queue & body]
  `(carmine/wcar (:conn ~queue)
     ~@body))

(defn flushall
  ([]
   (carmine/flushall))
  ([queue]
   (with-redis queue
     (flushall))))

(defn hget
  ([key field]
   (carmine/hget key field))
  ([queue key field]
   (with-redis queue
     (hget key field))))

(defn hset
  ([key field value]
   (carmine/hset key field value))
  ([queue key field value]
   (with-redis queue
     (hset key field value))))

(defn hsetnx
  ([key field value]
   (carmine/hsetnx key field value))
  ([queue key field value]
   (with-redis queue
     (hsetnx key field value))))

(defn zadd
  ([key score value]
   (carmine/zadd key score value))
  ([queue key score value]
   (with-redis queue
     (zadd key score value))))

(defn zrange
  ([key start stop opts]
   (carmine/zrange key start stop opts))
  ([queue key start stop opts]
   (with-redis queue
     (zrange key start stop opts))))

(defn zremrangebyrank
  ([key start stop]
   (carmine/zremrangebyrank key start stop))
  ([queue key start stop]
   (with-redis queue
     (zremrangebyrank key start stop))))

(defn multi
  ([]
   (carmine/multi))
  ([queue]
   (with-redis queue
     (multi))))

(defn exec
  ([]
   (carmine/exec))
  ([queue]
   (with-redis queue
     (exec))))

(defmacro atomic
  [queue & body]
  `(last
     (with-redis ~queue
       (multi)
       ~@body
       (exec))))
