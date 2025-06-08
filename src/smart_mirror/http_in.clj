(ns smart-mirror.http-in
  (:require [common.time :as time]
            [smart-mirror.adapters.out :as out.adapter]
            [smart-mirror.controller :as controller]))

(defn time-handler
  [{{:keys [include]} :query-params}]
   {:status 200
    :body (controller/now include (time/now))})

(defn weather-handler
  [{{:keys [http-client]} :components}]
  {:status 200
   :body (-> http-client
             controller/weather-forecast
             out.adapter/weather-forecast->wire)})

(defn calendar-handler
  [{{:keys [http-client config]} :components}]
  {:status 200
   :body (-> http-client
             (controller/gcal-events config (time/now)))})

(def routes
  ["/time" {:get {:handler time-handler}}
   "/weather" {:get {:handler weather-handler}}
   "/calendar" {:get {:handler calendar-handler}}])

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
