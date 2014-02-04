(ns cuckoo.cron.parse-test
  (:use clojure.test
        [cuckoo.cron.parse :only [parse]])
  (:import java.util.Date))

(deftest parse-test
  (testing "with a 7-component cron"
    (testing "with all wildcards"
      (testing "expands to complete ranges"
        (is (= {:year        (set (range 1970 2100))
                :month       (set (range 1 13))
                :day         (set (range 1 32))
                :hour        (set (range 0 24))
                :minute      (set (range 0 60))
                :second      (set (range 0 60))}
               (parse "* * * * * * *"
                      (Date. (- 2014 1900) 0 1))))))

    (testing "with an exact second"
      (testing "expands to the single second"
        (is (= #{10}
               (:second (parse "10 * * * * * *"
                               (Date. (- 2014 1900) 0 1)))))))

    (testing "with a list of seconds"
      (testing "expands to the listed set"
        (is (= #{10 20 30}
               (:second (parse "10,20,30 * * * * * *"
                               (Date. (- 2014 1900) 0 1)))))))

    (testing "with a range of seconds"
      (testing "expands to the set in the range"
        (is (= #{15 16 17 18 19 20}
               (:second (parse "15-20 * * * * * *"
                               (Date. (- 2014 1900) 0 1)))))))

    (testing "with wildcard seconds in steps"
      (testing "expands to the set in the full range using the step"
        (is (= #{0 4 8 12 16 20 24 28 32 36 40 44 48 52 56}
               (:second (parse "*/4 * * * * * *"
                               (Date. (- 2014 1900) 0 1)))))))

    (testing "with a range of seconds in steps"
      (testing "expands to the set in the range using the step"
        (is (= #{0 10 20 30}
               (:second (parse "0-30/10 * * * * * *"
                               (Date. (- 2014 1900) 0 1)))))))

    (testing "with a mix of formats for seconds"
      (testing "expands to the unionized set"
        (is (= #{3 4 7 10 12 14 40}
               (:second (parse "3,4,7,10-15/2,40 * * * * * *"
                               (Date. (- 2014 1900) 0 1)))))))

    (testing "with an exact day"
      (testing "expands to the single day"
        (is (= #{3}
               (:day (parse "* * * * 3 * *"
                            (Date. (- 2014 1900) 0 1)))))))

    (testing "with a wildcard day in feb"
      (testing "expands to the set in the range 1-28"
        (is (= (set (range 1 29))
               (:day (parse "* * * * * * *"
                            (Date. (- 2014 1900) 1 1)))))))

    (testing "with a wildcard day in a leap year feb"
      (testing "expands to the set in the range 1-29"
        (is (= (set (range 1 30))
               (:day (parse "* * * * * * *"
                            (Date. (- 2016 1900) 1 1)))))))

    (testing "with a wildcard day in june"
      (testing "expands to the set in the range 1-30"
        (is (= (set (range 1 31))
               (:day (parse "* * * * * * *"
                            (Date. (- 2014 1900) 5 1)))))))

    (testing "with a wildcard day in steps"
      (testing "expands to the set in the range using step"
        (is (= #{1 5 9 13 17 21 25}
               (:day (parse "* * * * */4 * *"
                            (Date. (- 2014 1900) 1 1)))))

        (is (= #{1 5 9 13 17 21 25 29}
               (:day (parse "* * * * */4 * *"
                            (Date. (- 2016 1900) 1 1)))))))

    (testing "with a single day-of-week integer"
      (testing "expands to the equivalent days of month"
        (is (= #{2 9 16 23}
               (:day (parse "* * * * * 0 *"
                            (Date. (- 2014 1900) 1 1)))))))

    (testing "with L in the day field"
      (testing "as a single value"
        (testing "expands to the last day of the month"
          (is (= #{28} (:day (parse "* * * * L * *"
                                    (Date. (- 2014 1900) 1 1)))))
          (is (= #{29} (:day (parse "* * * * L * *"
                                    (Date. (- 2016 1900) 1 1)))))))

      (testing "as part of a list"
        (testing "expands to the last day of the month in the list"
          (is (= #{1 15 28} (:day (parse "* * * * 1,15,L * *"
                                         (Date. (- 2014 1900) 1 1)))))
          (is (= #{1 15 29} (:day (parse "* * * * 1,15,L * *"
                                         (Date. (- 2016 1900) 1 1)))))))

      (testing "as part of a range"
        (testing "expands to the last day of the month in the range"
          (is (= (set (range 15 29))
                 (:day (parse "* * * * 15-L * *" (Date. (- 2014 1900) 1 1)))))
          (is (= (set (range 15 30))
                 (:day (parse "* * * * 15-L * *" (Date. (- 2016 1900) 1 1))))))))

    (testing "parsing a complex string"
      (is (= {:year        #{2014 2015 2016 2017}
              :month       #{1 2 3 4 5 6 7 8 9 10 11 12}
              :day         #{1 3 5 7 9 20 23 26 29}
              :hour        #{3 4 5 17 19}
              :minute      (set (range 0 60))
              :second      #{0 10 20 30 40 50}}
             (parse "*/10 * 3-5,17,19 * 1-10/2,20-L/3 * 2014-2017"
                    (Date. (- 2014 1900) 0 1))))))

  (testing "with a 6-component cron"
    (testing "uses seconds as first field and sets the year to the full range"
      (is (= {:year        (set (range 1970 2100))
              :month       #{7 8}
              :day         #{5 6 12 13 19 20 26 27 9 10}
              :hour        #{5 6}
              :minute      #{3 4}
              :second      #{1 2}}
             (parse "1,2 3,4 5,6 7,8 9,10 0,1" (Date. (- 2014 1900) 0 1))))))

  (testing "with a traditional 5-component cron"
    (testing "sets second to zero and the year to the full range"
      (is (= {:year        (set (range 1970 2100))
              :month       #{5 6}
              :day         #{5 6 12 13 19 20 26 27 7 8}
              :hour        #{3 4}
              :minute      #{1 2}
              :second      #{0}}
             (parse "1,2 3,4 5,6 7,8 0,1" (Date. (- 2014 1900) 0 1))))))

  (testing "with too few cron fields throws IllegalArgumentException"
    (is (thrown? IllegalArgumentException (parse "* * * *"))))

  (testing "with too many cron fields throws IllegalArgumentException"
    (is (thrown? IllegalArgumentException (parse "* * * * * * * *")))))
