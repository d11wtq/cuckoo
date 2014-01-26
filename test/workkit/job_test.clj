(ns workkit.job-test
  (:require [workkit.job :as job])
  (:use clojure.test))

(deftest job-test
  (testing "workkit.job/id"
    (testing "is alphanumeric"
      (is (re-find #"^[a-z0-9]+$"
                   (job/id "* * * * * * *" #'println ["foo"]))))

    (testing "for a single job is always the same"
      (is (= (job/id "* * * * * * *" #'println ["foo"])
             (job/id "* * * * * * *" #'println ["foo"]))))

    (testing "for a unique jobs is different"
      (is (not= (job/id "* * * * * * *" #'println ["foo"])
                (job/id "* * * * * * *" '#(.toUpperCase %) ["foo"]))))

    (testing "for a unique cron strings is different"
      (is (not= (job/id "* * * * * * *" #'println ["foo"])
                (job/id "* * 1 * * * *" #'println ["foo"]))))

    (testing "for a unique job args is different"
      (is (not= (job/id "* * * * * * *" #'println ["foo"])
                (job/id "* * * * * * *" #'println ["bar"])))))

  (testing "workkit.job/payload"
    (testing "is JSON"
      (is (= "{\"cron\":\"* * * * * * *\",\"job\":\"#'clojure.core\\/println\",\"args\":[\"foo\"]}"
             (job/payload "* * * * * * *" #'println ["foo"]))))))
