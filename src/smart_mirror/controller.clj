(ns smart-mirror.controller
  (:require [common.exceptions]
            [common.protocols.config :as protocols.config]
            [common.protocols.gauth :as protocols.gauth]
            [smart-mirror.coordinates :as coordinates]
            [smart-mirror.db.plants :as db.plants]
            [smart-mirror.http-out :as http-out]
            [smart-mirror.time :as time]))

(defn weather-forecast
  [http-client]
  (let [location (http-out/get-location-from-ip http-client)
        coordinates (-> location
                        :loc
                        coordinates/->coordinates)
        timezone (:timezone location)]
    (http-out/get-weather-forecast http-client coordinates timezone)))

(defn now
  [include as-of]
  (if-let [timezones (time/qs->timezones include)]
    (time/time+zones as-of timezones)
    (common.exceptions/bad-request "invalid timezone")))


(defn gcal-events
  [http-client config as-of google-auth-token-provider]
  (let [min-time (time/->iso-start-of-day-utc as-of)
        max-time (time/->iso-end-of-day-utc as-of)
        access-token (protocols.gauth/get-access-token google-auth-token-provider config)]
    (http-out/get-gcal-events http-client access-token min-time max-time)))

(defn create-plant
  [database plant-data]
  {:plant-id (->> plant-data
                  (db.plants/create-plant! database))})

(defn get-all-plants
  [database]
  (db.plants/get-all-plants database))

(defn get-plant
  [database plant-id]
  (let [plant (db.plants/get-plant database plant-id)]
    (when (nil? plant)
      (common.exceptions/not-found (str "Plant with id " plant-id " not found")))
    plant))

(defn update-plant
  [database plant-id updates]
  (when (nil? (db.plants/get-plant database plant-id))
    (common.exceptions/not-found (str "Plant with id " plant-id " not found")))

  {:plant-id (db.plants/update-plant! database plant-id updates)})

(defn delete-plant
  [database plant-id]
  (when (nil? (db.plants/get-plant database plant-id))
    (common.exceptions/not-found (str "Plant with id " plant-id " not found")))

  (db.plants/delete-plant! database plant-id))

(defn water-plant
  [database plant-id watering-data]
  (when (nil? (db.plants/get-plant database plant-id))
    (common.exceptions/not-found (str "Plant with id " plant-id " not found")))

  (db.plants/record-watering! database plant-id watering-data))

(defn plants-due-for-watering
  [database http-client config as-of]
  (let [fcm-token (protocols.config/read-value config :fcm-token)]
    (when (some? (db.plants/get-plants-due-today database as-of))
      (http-out/send-notification! http-client fcm-token)
      (db.plants/register-notification! database as-of))))

(defn get-watering-history
  [database plant-id]
  (when (nil? (db.plants/get-plant database plant-id))
    (common.exceptions/not-found (str "Plant with id " plant-id " not found")))

  (db.plants/get-watering-history database plant-id))

