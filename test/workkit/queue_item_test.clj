(ns workkit.queue-item-test
  (:require [workkit.queue-item :as queue-item])
  (:use clojure.test)
  (:import java.util.Date))

(deftest queue-item-test
  (testing "workkit.queue-item/dump-str"
    (testing "returns a json representation without the date"
      (is (= "{\"id\":\"test\"}"
             (queue-item/dump-str {:id "test" :date (Date.)})))))

  (testing "workkit.queue-item/load-str"
    (testing "returns a clojure map"
      (is (= {:id "test"}
             (queue-item/load-str "{\"id\":\"test\"}")))))

  (testing "workkit.queue-item/dump-score"
    (testing "returns a stringified long integer"
      (is (= "1388534400000"
             (queue-item/dump-score (Date. (- 2014 1900) 0 1))))))

  (testing "workkit.queue-item/load-score"
    (testing "parses a stringified long integer to a Date"
      (is (= (Date. (- 2014 1900) 0 1)
             (queue-item/load-score "1388534400000"))))))
