(ns smart-mirror.http-in
  (:require [smart-mirror.controller :as controller]))

(defn foo-handler
  [request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (controller/foo request nil)})

(def route-map
  ["/version" {:get {:handler foo-handler}}
   "/health" {:get {:handler foo-handler}}
   "/metrics" {:get {:handler foo-handler}}])
