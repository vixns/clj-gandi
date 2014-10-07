(ns clj-gandi.core
  (:require [necessary-evil.core :as xml-rpc]
            [necessary-evil.value :refer [allow-nils]]
            [necessary-evil.fault :refer [fault?]]
            [throttler.core :refer [throttle-chan]]
            [environ.core :refer [env]]
            [clj-time.format :as format]
            [com.netflix.hystrix.core :as hystrix]
            [clojure.tools.logging :as log]
            [clj-logging-config.log4j :as log-config]
            [clojure.core.async :as async :refer (<!! >! <! put! close! chan go-loop alts!! timeout)]))

(defn- api-url [] (if (= "1" (env :gandi-prod))
                    "https://rpc.gandi.net/xmlrpc/"
                    "https://rpc.ote.gandi.net/xmlrpc/"))
(defn- api-key [] (or (env :gandi-api-key)
                      (log/fatal "GANDI_API_KEY env var missing")))
(def custom-date-formatter (format/formatter "yyyy-MM-dd HH:mm:ss"))
(def in (chan 1))
(def latency (Integer/parseInt (or (env :gandi-api-timeout-ms) "30000")))
(def retry-count (Integer/parseInt (or (env :gandi-api-retry-count) "5")))
(def slow-chan (throttle-chan in 14 :second))

(log-config/set-logger! "clj-gandi.core" :level (or (keyword (env :gandi-log-level)) :warn))
(System/setProperty "hystrix.command.clj-gandi.core/call*.execution.isolation.thread.timeoutInMilliseconds" (str latency))

(hystrix/defcommand call*
                    "gandi api wrapper.
                    (call :method :version.info)
                    (call :method :domain.list :items_per_page 500)
                    "
                    {:hystrix/fallback-fn (constantly nil)}
                    [{:keys [method] :or [:version.info] :as options}]
                    (allow-nils true (xml-rpc/call
                                       (api-url) method (api-key)
                                       (dissoc options :method :resp-ch))))


(defn- init-worker
  "init worker"
  [id]
  (go-loop []
           (let [e (<! slow-chan)]
             (log/debugf "message to gandi worker %s: %s" id e)
             (let [r (call* e)]
               (log/debugf "response from gandi worker %s: %s " id (or (:fault-string r) r))
               (if (fault? r) (log/errorf "Error in rpc call %s" (str (:fault-string r) e)))
               (if-let [c (:resp-ch e)]
                 (if-not (nil? r) (>! c r)))))
           (recur)))

(defn initialize
  "initialize workers"
  ([] (initialize 40))
  ([n] (dorun (map (partial init-worker) (range n)))))

(defn call
  "get gandi ressources by method"
  ([method] (call method nil))
  ([method options]
   (let [r (chan)
         req (merge {:method method :resp-ch r} options)]
     (loop [retry 1]
       (put! in req)
       (let [a (alts!! [r (timeout latency)])
             resp (first a)]
         (log/debugf "call response on try %s: %s" retry resp)
         (if (nil? resp)
           (if (< retry retry-count)
             (recur (inc retry))
             (do (close! r) (log/errorf "Error in rpc call %s" (str (:fault-string resp) req))))
           (do (close! r) (identity resp)))))
     )))

(defn list-all
  "list all gandi ressources by method."
  ([method] (list-all method nil))
  ([method options]
   (loop [page 0 ret nil]
     (let [ipp 500
           res (call method (merge {:page page :items_per_page ipp} options))
           r (concat ret res)]
       (if (< (count res) ipp) r (recur (inc page) r))))))

(defn map-all
  "map function f on api call result, page per page"
  [method options f]
  (loop [page 0]
    (let [ipp 100
          res (call method (merge {:page page :items_per_page ipp} options))]
      (future (dorun (pmap f res)))
      (if (= (count res) ipp) (recur (inc page))))))


;;; introspection

(defn methods-list
  "list all available methods, do not use workers pool."
  [] (xml-rpc/call (api-url) :system.listMethods))

(defn method-help
  "get method help, do not use workers pool."
  [method] (xml-rpc/call (api-url) :system.methodHelp (name method)))

(defn method-signature
  "get method signature, do not use workers pool."
  [method] (xml-rpc/call (api-url) :system.methodSignature (name method)))