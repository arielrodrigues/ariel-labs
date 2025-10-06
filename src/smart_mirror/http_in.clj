(ns smart-mirror.http-in
  (:require [common.time :as time]
            [smart-mirror.adapters.in :as in.adapter]
            [smart-mirror.adapters.out :as out.adapter]
            [smart-mirror.controller :as controller]
            [smart-mirror.specs.in :as in]))

(defn time-handler
  [{{:keys [include]} :query-params}]
   {:status 200
    :body (-> (controller/now include (time/now))
              out.adapter/times->wire)})

(defn weather-handler
  [{{:keys [http-client]} :components}]
  {:status 200
   :body (-> http-client
             controller/weather-forecast
             out.adapter/weather-forecast->wire)})

(defn calendar-handler
  [{{:keys [http-client config google-auth-token-provider]} :components}]
  {:status 200
   :body (-> http-client
             (controller/gcal-events config (time/now) google-auth-token-provider)
             out.adapter/calendar->wire)})

(defn create-plant-handler
  [{{:keys [database]} :components
    plant-data :json-params}]
  (let [internal-data (in.adapter/wire->create-plant plant-data)]
    {:status 201
     :body (controller/create-plant database internal-data)}))

(defn get-plants-handler
  [{{:keys [database]} :components}]
  (let [plants (controller/get-all-plants database)]
    {:status 200
     :body (out.adapter/plants->wire plants)}))

(defn get-plant-handler
  [{{:keys [database]} :components
    {:keys [id]} :path-params}]
  (let [plant (controller/get-plant database id)]
    (if plant
      {:status 200
       :body (out.adapter/plant->wire plant)}
      {:status 404
       :body {:message "Plant not found"}})))

(defn update-plant-handler
  [{{:keys [database]} :components
         {:keys [id]} :path-params
         updates :json-params}]
  (let [internal-updates (in.adapter/wire->update-plant updates)]
    {:status 200
     :body (controller/update-plant database id internal-updates)}))

(defn delete-plant-handler
  [{{:keys [database]} :components
         {:keys [id]} :path-params}]
  (controller/delete-plant database id)
  {:status 204})

(defn water-plant-handler
  [{{:keys [database]} :components
    {:keys [id]} :path-params
    watering-data :json-params}]
  (try
    (let [internal-data (in.adapter/wire->water-plant watering-data)
          watering-id (controller/water-plant database id internal-data)]
      {:status 201
       :body {:watering-id watering-id}})
    (catch Exception e
      (println "ERROR in water-plant-handler:" (.getMessage e))
      (.printStackTrace e)
      {:status 500 :body {:message "Internal server error."}})))

(defn get-plants-due-handler
  [{{:keys [database]} :components}]
  (let [plants (controller/get-plants-due-for-watering database)]
    {:status 200
     :body (out.adapter/plants->wire plants)}))

(defn get-watering-history-handler
  [{{:keys [database]} :components
         {:keys [id]} :path-params}]
  (let [history (controller/get-watering-history database id)]
    {:status 200
     :body (out.adapter/waterings->wire history)}))

(def routes
  ["/time" {:get {:handler time-handler}}
   "/weather" {:get {:handler weather-handler}}
   "/calendar" {:get {:handler calendar-handler}}
   "/plants" {:get {:name :get-plants
                    :handler get-plants-handler}
              :post {:name :create-plant
                     :handler create-plant-handler}}
   "/plants/need-watering" {:get {:name :get-plants-due
                                         :handler get-plants-due-handler}}
   "/plants/:id/water" {:post {:name :water-plant
                               :handler water-plant-handler}}
   "/plants/:id/history" {:get {:name :get-watering-history
                                :handler get-watering-history-handler}}
   "/plants/:id" {:get {:name :get-plant
                        :handler get-plant-handler}
                  :put {:name :update-plant
                        :handler update-plant-handler}
                  :delete {:name :delete-plant
                           :handler delete-plant-handler}}])

;; Route validation specs mapping
(def route-validation-specs
  {:create-plant ::in/create-plant-request
   :update-plant ::in/update-plant-request
   :water-plant ::in/water-plant-request})

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
