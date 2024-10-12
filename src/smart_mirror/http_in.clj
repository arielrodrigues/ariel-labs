(ns smart-mirror.http-in
  (:require
   [common.interceptors :as common.interceptors]
   [common.time :as time]
   [smart-mirror.controller :as controller]))

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

;; ---- base routes ----
;; @TODO: probably move it to the http server component

(defn health [_request]
  {:status 200
   :body {:http-server "I'm doing fine."}})

(defn version [_request]
  {:status 200
   :body {:version "0.1"}})

(defn metrics [_request]
  {:status 200
   :body []})

(def base-routes
  ["/version" {:get {:handler version}}
   "/health" {:get {:handler health}}
   "/metrics" {:get {:handler metrics}}])

(def route-map
  (-> routes
      (concat base-routes)
      common.interceptors/routes->routes+common-interceptors))
