(defproject clj-gandi "0.1.0"
  :description "Gandi Api wrapper"
  :url "http://github.com/vixns/clj-gandi"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src"]
  :resource-paths ["resources"]
  :repositories [["local" "file:///Users/kaalh/Development/jars"]]
  :dependencies [
                  [org.clojure/clojure "1.6.0"]
                  [org.clojure/core.async "0.1.338.0-5c5012-alpha"]
                  [environ "1.0.0"]
                  [throttler "1.0.0"]
                  [necessary-evil "2.0.0"]
                  [cheshire "5.3.1"]
                  [com.taoensso/timbre "3.2.1"]
                  [circuit-breaker "0.1.7"]
                  ]
  :plugins [[lein-environ "1.0.0"]]
  :jvm-opts ["-Djava.awt.headless=true" "-Djava.library.path=/usr/local/lib:/opt/local/lib:/usr/lib"]
  )
