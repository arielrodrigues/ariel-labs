(ns smart-mirror.http-in
  (:require
   [common.interceptors :as common.interceptors]
   [common.time :as time]
   [smart-mirror.controller :as controller]))

(defn foo-handler
  [request]
  {:status 200
   :body (controller/foo request nil)})

(defn now!
  [request]
  {:status 200
   :body (controller/now request (time/now))})

(def base-routes
  ["/version" {:get {:handler foo-handler}}
   "/health" {:get {:handler foo-handler}}
   "/metrics" {:get {:handler foo-handler}}])

(def routes
  ["/now" {:get {:handler now!}}])

(def route-map
  (-> routes
      (concat base-routes)
      common.interceptors/routes->routes+common-interceptors))
