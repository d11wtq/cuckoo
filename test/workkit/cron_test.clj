(ns workkit.cron-test
  (:require [workkit.cron :as cron])
  (:use clojure.test)
  (:import java.util.Date))

(deftest cron-test
  (testing "workkit.cron/build"
    (testing ":at"
      (testing "with an exact date"
        (testing "sets all fields"
          (is (= "2014 3 2 7 9 0 0" ; 7 = Sun
                 (cron/build :at "2014-03-02T09:00:00Z")))))))

  (testing "workkit.cron/next-time"
    (testing "with all fields set"
      (testing "returns the exact date"
        (is (= (Date. (- 2014 1900) (- 3 1) 2 9 0 0)
               (cron/next-time "2014 3 2 7 9 0 0"
                               (Date. (- 2014 1900) (- 3 1) 2 8 0 0))))))))
