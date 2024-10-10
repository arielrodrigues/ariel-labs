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
    (assoc component :server (http/stop server))))

(defn new-http-server
  ([routes]
   (new-http-server routes DEFAULT-SERVER-PORT))
  ([routes port]
   (map->HttpServer {:routes routes :port port})))

(defrecord MockHttpServer [routes port server]
  component/Lifecycle
  (start [component]
    (let [service-map {::http/routes (expand-routes! routes)
                       ::http/type :jetty
                       ::http/port port}
          instance (-> service-map
                       io.pedestal.http/create-servlet
                       :io.pedestal.http/service-fn)]
      (assoc component :service instance))))

(defn new-mock-http-server
  [routes]
  (map->MockHttpServer {:routes routes :port DEFAULT-SERVER-PORT}))
