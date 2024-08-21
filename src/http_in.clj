(ns smart-mirror.http-in
  (:require [smart-mirror.controller :as controller]))

(defn foo-handler
  [request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (controller/foo request nil)})
