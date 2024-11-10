(ns smart-mirror.adapters.in
  (:require [clojure.spec.alpha :as s]
            [smart-mirror.specs.in :as in]
            [smart-mirror.weather :as weather]))

;; weather
(defn- ->normalized-weather-keys
  [namespace]
  (let [->namespaced (fn [n] (keyword namespace n))
        ->normalized-keys
        {:temperature_2m (->namespaced "temperature")
         :temperature_2m_max (->namespaced "temperature-max")
         :temperature_2m_min (->namespaced "temperature-min")
         :relative_humidity_2m (->namespaced "relative-humidity")
         :apparent_temperature (->namespaced "apparent-temperature")
         :precipitation (->namespaced "precipitation")
         :precipitation_probability (->namespaced "precipitation-probability")
         :precipitation_probability_max (->namespaced "precipitation-probability-max")
         :precipitation_hours (->namespaced "precipitation-hours")
         :rain (->namespaced "rain")
         :rain_sum (->namespaced "rain-sum")
         :snowfall (->namespaced "snowfall")
         :snowfall_sum (->namespaced "snowfall-sum")
         :wind_speed_10m (->namespaced "wind-speed")
         :visibility (->namespaced "visibility")
         :uv_index (->namespaced "uv-index")
         :sunshine_duration (->namespaced "sunshine-duration")
         :sunset (->namespaced "sunset")
         :sunrise (->namespaced "sunrise")
         :daylight_duration (->namespaced "daylight-duration")
         :hour_h (->namespaced "hour")
         :time (->namespaced "time")}]
    (fn [k] (get ->normalized-keys k k))))

(defn- ->current-weather-forecast
  [raw]
  (-> raw
      (update-keys (->normalized-weather-keys "smart-mirror.weather"))
      (dissoc :interval)))

(defn- ->hourly-weather-forecast
  [raw]
  (update-keys raw (->normalized-weather-keys "smart-mirror.weather.hourly")))

(defn- ->daily-weather-forecast
  [raw]
  (update-keys raw (->normalized-weather-keys "smart-mirror.weather.daily")))

(s/fdef wire->weather-forecast
  :args (s/cat :forecast ::in/weather-forecast)
  :ret  ::weather/forecast)
(defn wire->weather-forecast
  [{:keys [latitude longitude elevation current hourly daily]}]
  #::weather{:latitude latitude
             :longitude longitude
             :elevation elevation
             :current (->current-weather-forecast current)
             :hourly (->hourly-weather-forecast hourly)
             :daily (->daily-weather-forecast daily)
             :default-units weather/default-units})
