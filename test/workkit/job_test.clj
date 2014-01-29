(ns workkit.job-test
  (:require [workkit.job :as job])
  (:use clojure.test))

(deftest job-test
  (testing "workkit.job/id"
    (testing "is alphanumeric"
      (is (re-find #"^[a-z0-9]+$"
                   (job/id {:cron "* * * * * * *"
                            :job #'println
                            :args ["test"]}))))

    (testing "for a single job is always the same"
      (is (= (job/id {:cron "* * * * * * *"
                      :job #'println
                      :args ["test"]})
             (job/id {:cron "* * * * * * *"
                      :job #'println
                      :args ["test"]}))))

    (testing "for a unique jobs is different"
      (is (not= (job/id {:cron "* * * * * * *"
                         :job #'println
                         :args ["test"]})
                (job/id {:cron "* * * * * * *"
                         :job #(.toUpperCase %)
                         :args ["test"]}))))

    (testing "for a unique cron strings is different"
      (is (not= (job/id {:cron "* * * * * * *"
                         :job #'println
                         :args ["test"]})
                (job/id {:cron "* * 1 * * * *"
                         :job #'println
                         :args ["test"]}))))

    (testing "for a unique job args is different"
      (is (not= (job/id {:cron "* * * * * * *"
                         :job #'println
                         :args ["foo"]})
                (job/id {:cron "* * * * * * *"
                         :job #'println
                         :args ["bar"]})))))

  (testing "workkit.job/dump-str"
    (let [payload {:cron "* * * * * * *"
                   :job #'println
                   :args ["test"]}]
      (testing "returns a json representation"
        (is (= (format "{\"cron\":\"%s\",\"job\":\"%s\",\"args\":%s}"
                       "* * * * * * *"
                       "#'clojure.core\\/println"
                       "[\"test\"]")
               (job/dump-str payload))))))

  (testing "workkit.job/parse-str"
    (let [payload-str (format "{\"cron\":\"%s\",\"job\":\"%s\",\"args\":%s}"
                              "* * * * * * *"
                              "#'clojure.core\\/println"
                              "[\"test\"]")]
      (testing "returns a clojure map"
        (is (= {:cron "* * * * * * *"
                :job #'println
                :args ["test"]}
               (job/parse-str payload-str))))))

  (testing "workkit.job/run"
    (let [payload {:cron "* * * * * * *"
                   :job #(.toUpperCase %)
                   :args ["test"]}]
      (testing "applies fn to args"
        (is (= "TEST" (job/run payload)))))))
