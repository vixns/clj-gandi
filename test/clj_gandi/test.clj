(ns clj-gandi.test
  (require
    [clojure.test :refer :all]
    [taoensso.timbre :as timbre :refer [set-level!]]
    [clj-gandi.core :refer [call list-all]]))

(timbre/set-level! :warn)

(defn init [f]
  (clj-gandi.core/initialize)
  (f))

(use-fixtures :once init)

(deftest test-call
  (testing "api simple call tests"
    (is (contains? (call :version.info) :api_version))
    (is (not (nil? (call :domain.list))))
    ))

(deftest test-list
  (testing "list tests"
    (is (seq? (list-all :domain.list)))
    ))