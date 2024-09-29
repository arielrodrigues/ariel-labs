(ns common.http-server
  (:require [com.stuartsierra.component :as component]
            [common.routes :refer [expand-routes!]]
            [io.pedestal.http :as http]))

(def DEFAULT-SERVER-PORT 8080)

(defrecord HttpServer [routes port server]
  component/Lifecycle

  (start [component]
    (let [service-map {::http/routes (expand-routes! routes)
                       ::http/type :jetty
                       ::http/port port}
          instance (-> service-map
                       (assoc ::http/join? false)
                       http/create-server
                       http/start)]
      (assoc component :server instance)))

  (stop [component]
    (http/stop server)))

(defn new-http-server
  ([routes]
   (new-http-server routes DEFAULT-SERVER-PORT))
  ([routes port]
   (map->HttpServer {:routes routes :port port})))
