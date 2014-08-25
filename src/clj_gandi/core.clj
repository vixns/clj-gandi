(ns clj-gandi.core
  (:require [necessary-evil.core :as xml-rpc]
            [necessary-evil.value :refer [allow-nils]]
            [necessary-evil.fault :refer [fault?]]
            [throttler.core :refer [throttle-chan]]
            [taoensso.timbre :refer [fatal warnf debugf]]
            [environ.core :refer [env]]
            [clj-time.format :as format]
            [circuit-breaker.core :refer [wrap-with-circuit-breaker defncircuitbreaker]]
            [clojure.core.async :as async :refer (<!! >! <! put! close! chan go-loop)]))

(defn- api-url [] (if (= "1" (env :gandi-prod)) "https://rpc.gandi.net/xmlrpc/" "https://rpc.ote.gandi.net/xmlrpc/"))
(defn- api-key [] (or (env :gandi-api-key) (fatal "GANDI_API_KEY env var missing")))
(defncircuitbreaker :rpc {:timeout 10 :threshold 2})
(def custom-date-formatter (format/formatter "yyyy-MM-dd HH:mm:ss"))
(def in (chan 1))
(def slow-chan (throttle-chan in 15 :second))

(defn- call*
  "gandi api wrapper.
  (call :method :version.info)
  (call :method :domain.list :items_per_page 500)
  "
  [{:keys [method] :or [:version.info] :as options}]
  (try
    (let [res (allow-nils true (xml-rpc/call (api-url) method (api-key) (dissoc options :method :resp-ch :retry-count)))]
      (if (fault? res)
        (throw (Exception. (str (:fault-string res) method options)))
        (identity res)))
    (catch Exception e
      (do
        (debugf "caught exception: %s" (.getMessage e))
        (let [rc (or (:retry-count options) 0)]
          (if (< rc 5)
            (do
              (warnf "retry call %s %s " rc options)
              (Thread/sleep 300)
              (call* (assoc options :retry-count (inc rc))))
            (throw (Exception. (str "error in rpc call " (.getMessage e))))))))))

(defn- init-worker
  "init worker"
  [i]
  (go-loop []
           (let [e (<! slow-chan)
                 r (wrap-with-circuit-breaker :rpc
                                              (fn [] (debugf "message to gandi worker %s: %s" i e)
                                                (call* e)))]
             (debugf "response from gandi worker %s: %s" i r)
             (if (contains? e :resp-ch)
               (let [c (:resp-ch e)]
                 (if (nil? r) (close! c) (>! c r)))))
           (recur)))

(defn initialize
  "initialize workers"
  ([] (initialize 45))
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
