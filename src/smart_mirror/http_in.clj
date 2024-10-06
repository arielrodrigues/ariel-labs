(ns smart-mirror.http-in
  (:require
   [common.interceptors :as common.interceptors]
   [common.time :as time]
   [smart-mirror.controller :as controller]))

(defn foo-handler
  [request]
  {:status 200
   :body (controller/foo request nil)})

(defn time
  [request]
  {:status 200
   :body (controller/now request (time/now))})

(defn weather
  [request]
  {:status 200
   :body (controller/foo request nil)})

(def routes
  ["/time" {:get {:handler time}}
   "/weather" {:get {:handler weather}}])

(def base-routes
  ["/version" {:get {:handler foo-handler}}
   "/health" {:get {:handler foo-handler}}
   "/metrics" {:get {:handler foo-handler}}])

(def route-map
  (-> routes
      (concat base-routes)
      common.interceptors/routes->routes+common-interceptors))
