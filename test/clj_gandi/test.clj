(ns clj-gandi.test
  (require
    [clojure.test :refer :all]
    [clj-logging-config.log4j :as log-config]
    [clj-gandi.core :refer [call list-all]]))

(log-config/set-logger! "root" :level :warn)
;(log-config/set-logger! "clj-gandi.core" :level :debug)

(defonce gandi-pool (clj-gandi.core/initialize))

(deftest test-call
  (testing "api simple call tests"
    (is (contains? (call :version.info) :api_version))
    (is (not (nil? (call :domain.list))))
    (is (not (nil? (call :domain.list {:items_per_page 10 :page 0}))))
    ))

(deftest test-list
  (testing "list tests"
    (is (seq? (list-all :domain.list)))
    ))