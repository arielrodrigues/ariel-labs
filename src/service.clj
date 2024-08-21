(ns smart-mirror.service
  (:require [smart-mirror.http-in :as http-in]
            [io.pedestal.http.route :as route]))


(def routes
  (route/expand-routes
   #{["/health" :get foo-handler :route-name :health]
     ["/version" :get foo-handler :route-name :version]
     ["/metrics" :get foo-handler :route-name :metrics]}))
