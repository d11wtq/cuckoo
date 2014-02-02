(ns cuckoo.cron-test
  (:require [cuckoo.cron :as cron])
  (:use clojure.test)
  (:import java.util.Date))

(deftest cron-test
  (testing "cuckoo.cron/next-date"
    (let [date (Date. (- 2014 1900) 2 5 9 34 16)]
      (testing "with all-wildcards"
        (testing "returns the same date"
          (is (= date (cron/next-date "* * * * * * *" date)))))

      (testing "with a set second matching"
        (testing "returns the same date"
          (is (= date (cron/next-date "16 * * * * * *" date)))))

      (testing "with a set second before"
        (testing "advances to the second in the next minute"
          (is (= (Date. (- 2014 1900) 2 5 9 35 15)
                 (cron/next-date "15 * * * * * *" date)))))

      (testing "with a set second after"
        (testing "advances to the second in the same minute"
          (is (= (Date. (- 2014 1900) 2 5 9 34 17)
                 (cron/next-date "17 * * * * * *" date)))))

      (testing "with a choice of umatched seconds"
        (testing "advances to the next matching second"
          (is (= (Date. (- 2014 1900) 2 5 9 34 19)
                 (cron/next-date "10,14,19,20 * * * * * *" date)))))

      (testing "with set minute after"
        (testing "advances to the start of the minute"
          (is (= (Date. (- 2014 1900) 2 5 9 35 0)
                 (cron/next-date "* 35 * * * * *" date)))))

      (testing "with a set second and minute after"
        (testing "advances to the matching second and minute"
          (is (= (Date. (- 2014 1900) 2 5 9 35 20)
                 (cron/next-date "20 35 * * * * *" date)))))

      (testing "with an unmatchable date"
        (testing "returns nil"
          (is (nil? (cron/next-date "* * * * * * 2013" date)))))))

  (testing "cuckoo.cron/seq"))
