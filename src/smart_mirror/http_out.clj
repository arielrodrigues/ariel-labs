(ns smart-mirror.http-out
  (:require [common.exceptions]
            [common.http-client]
            [common.protocols.http-client :as protocols.http-client]
            [smart-mirror.adapters.in :as in.adapter]))

(defn get-location-from-ip
  [http-client]
  (try
    (->> {:method :get
          :url "https://ipinfo.io/json"}
         (protocols.http-client/req! http-client)
         :body)
    (catch Exception e
      (common.exceptions/bad-gateway "Unable to determine location from IP address"))))

;; https://open-meteo.com/en/docs#current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,rain,showers,snowfall,wind_speed_10m&hourly=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation_probability,precipitation,rain,showers,snowfall,visibility,uv_index,sunshine_duration&daily=temperature_2m_max,temperature_2m_min,sunrise,sunset,daylight_duration,rain_sum,snowfall_sum,precipitation_hours,precipitation_probability_max&timezone=Europe%2FLondon
(defn get-weather-forecast
  [http-client [lat long] timezone]
  (let [current "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,rain,showers,snowfall,wind_speed_10m,weather_code"
        hourly "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation_probability,precipitation,rain,showers,snowfall,visibility,uv_index,sunshine_duration"
        daily "temperature_2m_max,temperature_2m_min,sunrise,sunset,daylight_duration,rain_sum,snowfall_sum,precipitation_hours,precipitation_probability_max,weather_code"]
    (try
      (->> {:method :get
            :url "https://api.open-meteo.com/v1/forecast"
            :query-params {:latitude lat
                           :longitude long
                           :current current
                           :hourly hourly
                           :daily daily
                           :timezone timezone}
            :headers {"Accept-Encoding" "identity"}}
           (protocols.http-client/req! http-client)
           :body
           in.adapter/wire->weather-forecast)
      (catch Exception e
        (common.exceptions/bad-gateway "Unable to fetch weather forecast")))))

(defn get-gcal-events
  [http-client access-token min-time max-time]
  (try
    (->> {:method :get
          :url "https://www.googleapis.com/calendar/v3/calendars/primary/events"
          :query-params {:timeMin min-time
                         :timeMax max-time
                         :maxResults 10
                         :Singleevents true
                         :orderBy "startTime"}
          :headers {"Authorization" (str "Bearer " access-token)}}
         (protocols.http-client/req! http-client)
         :body
         in.adapter/wire->gcal)
    (catch Exception e
      (if (= 403 (-> (ex-data e) :common.http-client/cause :status))
        (common.exceptions/forbidden "Google Calendar API access denied - insufficient permissions")
        (common.exceptions/bad-gateway "Unable to fetch calendar events")))))

(defn send-notification!
  [http-client token]
  (try
    (->> {:method :post
          :body {:to "fake-client-token",
                 :notification {:title "It's time to water your plants 💧"
                                :body "Your plants are thirsty!"}}
          :url "https://fcm.googleapis.com/fcm/send"
          :headers {"Authorization" (str "key=" token)}}
         (protocols.http-client/req! http-client)
         :body)
    (catch Exception e
      (if (= 403 (-> (ex-data e) :common.http-client/cause :status))
        (common.exceptions/forbidden "Firebase Cloud Message access denied")
        (common.exceptions/bad-gateway "Unable to send push notification")))))
