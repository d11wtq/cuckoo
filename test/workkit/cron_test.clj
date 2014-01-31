(ns workkit.cron-test
  (:require [workkit.cron :as cron])
  (:use clojure.test)
  (:import java.util.Date))

(deftest cron-test
  (testing "workkit.cron/load-str"
    (testing "for a 5-component cron string"
      (testing "returns a map of all components"
        (is (every? #{:year
                      :month
                      :day
                      :day-of-week
                      :hour
                      :minute
                      :second}
                    (keys (cron/load-str "* * * * *")))))

      (testing "sets the :second to zero"
        (is (= [0]
               (:second (cron/load-str "* * * * *"))))))))
