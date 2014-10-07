(ns clj-gandi.hystrix.server
  (:require
    [compojure.core :refer :all]
    [compojure.handler :as handler]
    [compojure.route :as route]
    [aleph.http :refer [start-http-server wrap-ring-handler]]
    [hystrix-event-stream-clj.core :as hystrix-event])
  )

(defroutes app-routes
           (GET "/hystrix.stream" [] (hystrix-event/stream))
           (route/resources "/")
           (route/not-found "Not Found"))

(def app
  (handler/site app-routes))

(defn run
  [port]
  (start-http-server (wrap-ring-handler app) {:port (Integer. port)}))