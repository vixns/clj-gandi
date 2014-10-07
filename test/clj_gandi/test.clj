(ns clj-gandi.test
  (require
    [clojure.test :refer :all]
    [clj-gandi.core :refer [call list-all]]))

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