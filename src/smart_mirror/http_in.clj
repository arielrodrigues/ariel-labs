(ns smart-mirror.http-in
  (:require [common.time :as time]
            [smart-mirror.controller :as controller]))

(defn time
  [{{:keys [include]} :query-params}]
   {:status 200
    :body (controller/now include (time/now))})

(defn weather
  [{{:keys [http-client]} :components}]
  {:status 200
   :body (controller/foo http-client)})

(def routes
  ["/time" {:get {:handler time}}
   "/weather" {:get {:handler weather}}])

;; ---- base routes ----
;; @TODO: probably move it to the http server component

(defn health [_]
  {:status 200
   :body {:http-server "I'm doing fine."}})

(defn version [_]
  {:status 200
   :body {:version "0.1"}})

(defn metrics [_]
  {:status 200
   :body []})

(def base-routes
  ["/version" {:get {:handler version}}
   "/health" {:get {:handler health}}
   "/metrics" {:get {:handler metrics}}])

(def route-map
  (-> routes
      (concat base-routes)))
