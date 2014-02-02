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

  (testing "cuckoo.cron/seq"
    (let [date (Date. (- 2014 1900) 2 5 9 34 16)]
      (testing "with all wildcards"
        (let [cron-seq (cron/seq "* * * * * * *" date)]
          (testing "increments by the second"
            (is (= (Date. (- 2014 1900) 2 5 9 34 16) (first cron-seq)))
            (is (= (Date. (- 2014 1900) 2 5 9 34 17) (nth cron-seq 1)))
            (is (= (Date. (- 2014 1900) 2 5 9 40 23) (nth cron-seq 367))))))

      (testing "on every minute"
        (let [cron-seq (cron/seq "0 * * * * * *" date)]
          (testing "increments in 4 minutes on the zero-second"
            (is (= (Date. (- 2014 1900) 2 5 9 35 0) (first cron-seq)))
            (is (= (Date. (- 2014 1900) 2 5 9 36 0) (nth cron-seq 1)))
            (is (= (Date. (- 2014 1900) 2 5 10 2 0) (nth cron-seq 27))))))

      (testing "on every 4th minute of 17:00pm"
        (let [cron-seq (cron/seq "0 */4 17 * * * *" date)]
          (testing "increments in 4 minutes of the zero second of 17:00pm"
            (is (= (Date. (- 2014 1900) 2 5 17 0 0) (first cron-seq)))
            (is (= (Date. (- 2014 1900) 2 5 17 4 0) (nth cron-seq 1)))
            (is (= (Date. (- 2014 1900) 2 6 17 0 0) (nth cron-seq 15))))))

      (testing "on every 4th minute of 17:00pm on a specific date"
        (let [cron-seq (cron/seq "0 */4 17 3 5 * 2014" date)]
          (testing "increments in 4 minutes of the zero second of 17:00pm"
            (is (= (Date. (- 2014 1900) 2 5 17 0 0) (first cron-seq)))
            (is (= (Date. (- 2014 1900) 2 5 17 4 0) (nth cron-seq 1))))
          (testing "is a finite sequence"
            (is (= 15 (count cron-seq))
                (= (Date. (- 2014 1900) 2 5 17 56 0) (last cron-seq)))))))))
