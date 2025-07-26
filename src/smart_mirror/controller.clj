(ns smart-mirror.controller
  (:require [common.exceptions]
            [common.protocols.gauth :as protocols.gauth]
            [smart-mirror.coordinates :as coordinates]
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
