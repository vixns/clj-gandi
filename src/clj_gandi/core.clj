(ns clj-gandi.core
  (:require [necessary-evil.core :as xml-rpc]
            [necessary-evil.value :refer [allow-nils]]
            [necessary-evil.fault :refer [fault?]]
            [throttler.core :refer [throttle-chan]]
            [environ.core :refer [env]]
            [clj-time.format :as format]
            [com.netflix.hystrix.core :as hystrix]
            [clojure.tools.logging :as log]
            [clojure.core.async :as async :refer (<!! >! <! put! close! chan go-loop)]))

(defn- api-url [] (if (= "1" (env :gandi-prod))
                    "https://rpc.gandi.net/xmlrpc/"
                    "https://rpc.ote.gandi.net/xmlrpc/"))
(defn- api-key [] (or (env :gandi-api-key)
                      (log/fatal "GANDI_API_KEY env var missing")))
(def custom-date-formatter (format/formatter "yyyy-MM-dd HH:mm:ss"))
(def in (chan 1))
(def slow-chan (throttle-chan in 14 :second))

;;; raise rpc call timeouts to 5000ms, needed when asking hundreds of items
(System/setProperty "hystrix.command.clj-gandi.core/call*.execution.isolation.thread.timeoutInMilliseconds" "5000")

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
             (loop [retry 0]
               (let [r (call* e)]
                 (log/debugf "response from gandi worker %s (try %s): %s " id (inc retry) (if (fault? r) (:fault-string r) r))
                 (if (or (fault? r) (nil? r))
                   (if (< retry 4)
                     (do (Thread/sleep 500) (recur (inc retry)))
                     (do
                       (if-let [c (:resp-ch e)] (close! c))
                       (log/errorf "Error in rpc call %s" (str (:fault-string r) e))))
                   (if-let [c (:resp-ch e)] (>! c r))))))
           (recur)))

(defn initialize
  "initialize workers"
  ([] (initialize 40))
  ([n] (dorun (map (partial init-worker) (range n)))))

(defn call
  "get gandi ressources by method"
  ([method] (call method nil))
  ([method options]
   (let [r (chan)]
     (put! in (merge {:method method :resp-ch r} options))
     (<!! r))))

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