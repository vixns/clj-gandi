(defproject clj-gandi "0.1.1"
  :description "Gandi Api wrapper"
  :url "http://github.com/vixns/clj-gandi"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src"]
  :dependencies [
                  [org.clojure/clojure "1.6.0"]
                  [org.clojure/core.async "0.1.338.0-5c5012-alpha"]
                  [environ "1.0.0"]
                  [throttler "1.0.0"]
                  [necessary-evil "2.0.0"]
                  [org.clojure/tools.logging "0.3.0"]
                  [clj-logging-config "1.9.12"]
                  [org.slf4j/slf4j-log4j12 "1.7.7"]
                  [hystrix-event-stream-clj "0.1.3"]
                  [com.netflix.hystrix/hystrix-clj "1.4.0-RC4"]
                  ]
  :plugins [[lein-environ "1.0.0"]]
  :jvm-opts ["-Djava.awt.headless=true" "-Djava.library.path=/usr/local/lib:/opt/local/lib:/usr/lib"]
  )
