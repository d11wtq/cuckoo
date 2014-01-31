(ns workkit.cron-test
  (:require [workkit.cron :as cron])
  (:use clojure.test)
  (:import java.util.Date))

(deftest cron-test
  (testing "workkit.cron/load-str"
    (testing "for a 7-component cron"
      (testing ":second"
        (testing "supports integers"
          (is (= [30] (:second (cron/load-str "30 * * * * * *")))))

        (testing "supports *"
          (is (= [\*] (:second (cron/load-str "* * * * * * *"))))))

      (testing ":minute"
        (testing "supports integers"
          (is (= [15] (:minute (cron/load-str "* 15 * * * * *")))))

        (testing "supports *"
          (is (= [\*] (:minute (cron/load-str "* * * * * * *"))))))

      (testing ":hour"
        (testing "supports integers"
          (is (= [17] (:hour (cron/load-str "* * 17 * * * *")))))

        (testing "supports *"
          (is (= [\*] (:hour (cron/load-str "* * * * * * *"))))))

      (testing ":month"
        (testing "supports integers"
          (is (= [9] (:month (cron/load-str "* * * 9 * * *")))))

        (testing "supports *"
          (is (= [\*] (:month (cron/load-str "* * * * * * *"))))))

      (testing ":day"
        (testing "supports integers"
          (is (= [28] (:day (cron/load-str "* * * * 28 * *")))))

        (testing "supports *"
          (is (= [\*] (:day (cron/load-str "* * * * * * *"))))))

      (testing ":day-of-week"
        (testing "supports integers"
          (is (= [4] (:day-of-week (cron/load-str "* * * * * 4 *")))))

        (testing "supports *"
          (is (= [\*] (:day-of-week (cron/load-str "* * * * * * *"))))))

      (testing ":year"
        (testing "supports integers"
          (is (= [2015] (:year (cron/load-str "* * * * * * 2015")))))

        (testing "supports *"
          (is (= [\*] (:year (cron/load-str "* * * * * * *")))))))

    (testing "for a 5-component cron string"
      (testing "uses :second = 0, :year = *"
        (is (= {:year        [\*]
                :month       [3]
                :day         [4]
                :day-of-week [5]
                :hour        [2]
                :minute      [1]
                :second      [0]}
               (cron/load-str "1 2 3 4 5"))))))

  (testing "workkit.cron/seq"
    (testing "for the all-wildcard cron string"
      (testing "iterates by second"
        (let [start (Date. (- 2014 1900) (dec 2) 1 0 0 0)
              dates (cron/seq "* * * * * * *" start)]
          (is (= (Date. (- 2014 1900) (dec 2) 1 0 0 0) (first dates)))
          (is (= (Date. (- 2014 1900) (dec 2) 1 0 0 2) (nth dates 2)))
          (is (= (Date. (- 2014 1900) (dec 2) 2 0 0 7) (nth dates 86407))))))))
