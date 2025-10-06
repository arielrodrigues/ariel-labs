(ns common.http-server
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]))

(def DEFAULT-SERVER-PORT 8080)

(defrecord HttpServer [routes port server]
  component/Lifecycle
  (start [component]
    (let [service-map {::http/routes (:routes routes)
                       ::http/type :jetty
                       ::http/port port
                       ::http/router :linear-search}
          instance (-> service-map
                       (assoc ::http/join? false)
                       http/create-server
                       http/start)]
      (assoc component :server instance)))

  (stop [component]
    (assoc component :server (http/stop server))))

(defn new-http-server
  ([]
   (new-http-server DEFAULT-SERVER-PORT))
  ([port]
   (map->HttpServer {:port port})))

;; --

(defrecord MockHttpServer [routes port server]
  component/Lifecycle
  (start [component]
    (let [service-map {::http/routes (:routes routes)
                       ::http/type :jetty
                       ::http/port port
                       ::http/router :linear-search}
          instance (-> service-map
                       io.pedestal.http/create-servlet
                       :io.pedestal.http/service-fn)]
      (assoc component :service instance))))

(defn new-mock-http-server
  []
  (map->MockHttpServer {:port DEFAULT-SERVER-PORT}))
