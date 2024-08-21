(ns smart-mirror.server
  (:require [io.pedestal.http :as http]
            [smart-mirror.service :as service]
            [io.pedestal.test :as test]))

(def service-map
  {::http/routes service/routes
   ::http/type :jetty
   ::http/port 8080})

(defn start []
  (-> service-map
      http/create-server
      http/start))

;; For interactive development
(defonce server (atom nil))

(defn start-dev []
  (reset! server
          (http/start (http/create-server
                        (assoc service-map
                               ::http/join? false)))))

(defn stop-dev []
  (http/stop @server))

(defn restart []
  (stop-dev)
  (start-dev))
