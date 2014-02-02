(ns cuckoo.queue-item-test
  (:require [cuckoo.queue-item :as queue-item])
  (:use clojure.test)
  (:import java.util.Date))

(deftest queue-item-test
  (testing "cuckoo.queue-item/dump-str"
    (testing "returns a json representation without the date"
      (is (= "{\"id\":\"test\"}"
             (queue-item/dump-str {:id "test" :date (Date.)})))))

  (testing "cuckoo.queue-item/load-str"
    (testing "returns a clojure map"
      (is (= {:id "test"}
             (queue-item/load-str "{\"id\":\"test\"}")))))

  (testing "cuckoo.queue-item/dump-score"
    (testing "returns a stringified long integer"
      (is (= "1388534400000"
             (queue-item/dump-score (Date. (- 2014 1900) 0 1))))))

  (testing "cuckoo.queue-item/load-score"
    (testing "parses a stringified long integer to a Date"
      (is (= (Date. (- 2014 1900) 0 1)
             (queue-item/load-score "1388534400000"))))))
