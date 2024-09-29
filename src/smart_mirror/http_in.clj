(ns smart-mirror.http-in
  (:require [common.time :as time]
            [smart-mirror.controller :as controller]))

(defn foo-handler
  [request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (controller/foo request nil)})

(defn now-handler
  [request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (controller/now request (time/now))})

(def base-routes
  ["/version" {:get {:handler foo-handler}}
   "/health" {:get {:handler foo-handler}}
   "/metrics" {:get {:handler foo-handler}}])

(def route-map
  (merge ["/now" {:get {:handler now-handler}}]
         base-routes))
