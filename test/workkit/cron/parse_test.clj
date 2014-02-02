(ns workkit.cron.parse-test
  (:use clojure.test
        [workkit.cron.parse :only [parse]])
  (:import java.util.Date))

(deftest parse-test
  (testing "with a 7-component cron"
    (testing "with all wildcards"
      (testing "expands to complete ranges"
        (is (= {:year        (set (range 1970 2100))
                :month       (set (range 1 13))
                :day         (set (range 1 29))
                :day-of-week (set (range 0 7))
                :hour        (set (range 0 24))
                :minute      (set (range 0 60))
                :second      (set (range 0 60))}
               (parse "* * * * * * *"
                      (Date. (- 2014 1900) (dec 2) 1))))))

    (testing "with an exact second"
      (testing "expands to the single value"
        (is (= {:year        (set (range 1970 2100))
                :month       (set (range 1 13))
                :day         (set (range 1 29))
                :day-of-week (set (range 0 7))
                :hour        (set (range 0 24))
                :minute      (set (range 0 60))
                :second      #{10}}
               (parse "10 * * * * * *"
                      (Date. (- 2014 1900) (dec 2) 1))))))

    (testing "with a list of seconds"
      (testing "expands to the listed set"
        (is (= {:year        (set (range 1970 2100))
                :month       (set (range 1 13))
                :day         (set (range 1 29))
                :day-of-week (set (range 0 7))
                :hour        (set (range 0 24))
                :minute      (set (range 0 60))
                :second      #{10 20 30}}
               (parse "10,20,30 * * * * * *"
                      (Date. (- 2014 1900) (dec 2) 1))))))

    (testing "with a range of seconds"
      (testing "expands to the set in the range"
        (is (= {:year        (set (range 1970 2100))
                :month       (set (range 1 13))
                :day         (set (range 1 29))
                :day-of-week (set (range 0 7))
                :hour        (set (range 0 24))
                :minute      (set (range 0 60))
                :second      #{15 16 17 18 19 20}}
               (parse "15-20 * * * * * *"
                      (Date. (- 2014 1900) (dec 2) 1))))))

    (testing "with wildcard seconds/step"
      (testing "expands to the set in the full range using the step"
        (is (= {:year        (set (range 1970 2100))
                :month       (set (range 1 13))
                :day         (set (range 1 29))
                :day-of-week (set (range 0 7))
                :hour        (set (range 0 24))
                :minute      (set (range 0 60))
                :second      #{0 4 8 12 16 20 24 28 32 36 40 44 48 52 56}}
               (parse "*/4 * * * * * *"
                      (Date. (- 2014 1900) (dec 2) 1))))))

    (testing "with a range of seconds/step"
      (testing "expands to the set in the range using the step"
        (is (= {:year        (set (range 1970 2100))
                :month       (set (range 1 13))
                :day         (set (range 1 29))
                :day-of-week (set (range 0 7))
                :hour        (set (range 0 24))
                :minute      (set (range 0 60))
                :second      #{0 10 20 30}}
               (parse "0-30/10 * * * * * *"
                      (Date. (- 2014 1900) (dec 2) 1))))))

    (testing "with a mix of formats for seconds"
      (testing "expands to the unionized set"
        (is (= {:year        (set (range 1970 2100))
                :month       (set (range 1 13))
                :day         (set (range 1 29))
                :day-of-week (set (range 0 7))
                :hour        (set (range 0 24))
                :minute      (set (range 0 60))
                :second      #{3 4 7 10 12 14 40}}
               (parse "3,4,7,10-15/2,40 * * * * * *"
                      (Date. (- 2014 1900) (dec 2) 1))))))))
