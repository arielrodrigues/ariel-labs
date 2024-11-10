(ns smart-mirror.specs.in
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [common.time :as time]))

;; weather

(s/def ::latitude (s/with-gen float? #(gen/double* {:min -90 :max 90})))
(s/def ::longitude (s/with-gen float? #(gen/double* {:min -180 :max 180})))
(s/def ::elevation (s/with-gen float? #(gen/double* {:min 0 :max 10000})))

(defn date-gen [] (gen/fmap time/->local-date (gen/tuple (gen/choose 2018 2028) (gen/choose 1 12) (gen/choose 1 28))))
(defn hour-gen [] (gen/fmap time/->local-time (gen/tuple (gen/choose 0 23))))
(s/def ::hourly-interval (s/with-gen string?
                           (fn [] (let [date (date-gen)
                                       hour (hour-gen)]
                                    (gen/return (str (gen/generate date) "T" (gen/generate hour)))))))

(s/def ::interval (s/with-gen integer? #(gen/choose 0 1000)))
(s/def ::temperature_2m (s/with-gen float? #(gen/double* {:min -52 :max 42 :NaN? false})))
(s/def ::relative_humidity_2m (s/with-gen integer? #(gen/large-integer* {:min 5 :max 100})))
(s/def ::apparent_temperature (s/with-gen float? #(gen/double* {:min -52 :max 42 :NaN? false})))
(s/def ::precipitation (s/with-gen float? #(gen/double* {:min 0 :max 100 :NaN? false})))
(s/def ::precipitation_probability (s/with-gen integer? #(gen/choose 0 100)))
(s/def ::rain (s/with-gen float? #(gen/double* {:min 0 :max 100 :NaN? false})))
(s/def ::snowfall (s/with-gen float? #(gen/double* {:min 0 :max 30 :NaN? false})))
(s/def ::wind_speed_10m (s/with-gen float? #(gen/double* {:min 5 :max 60 :NaN? false})))
(s/def ::visibility (s/with-gen float? #(gen/double* {:min 10000 :max 50000 :NaN? false})))
(s/def ::uv_index (s/with-gen float? #(gen/double* {:min 0 :max 10 :NaN? false})))
(s/def ::sunshine_duration (s/with-gen float? #(gen/double* {:min 1 :max 14 :NaN? false})))
(s/def ::daylight_duration (s/with-gen float? #(gen/double* {:min 10000 :max 60000 :NaN? false})))
(s/def ::hour_h (s/with-gen float? #(gen/fmap float (gen/choose 0 23))))

(s/def ::current (s/keys :req-un [::interval
                                  ::temperature_2m
                                  ::relative_humidity_2m
                                  ::apparent_temperature
                                  ::precipitation
                                  ::rain
                                  ::snowfall
                                  ::wind_speed_10m]))

;; hourly
(s/def :smart-mirror.weather.hourly/time (s/coll-of ::hourly-interval :gen-max 5))
(s/def :smart-mirror.weather.hourly/temperature_2m (s/coll-of ::temperature_2m :gen-max 5))
(s/def :smart-mirror.weather.hourly/relative_humidity_2m (s/coll-of ::relative_humidity_2m :gen-max 5))
(s/def :smart-mirror.weather.hourly/apparent_temperature (s/coll-of ::apparent_temperature :gen-max 5))
(s/def :smart-mirror.weather.hourly/precipitation_probability (s/coll-of ::precipitation_probability :gen-max 5))
(s/def :smart-mirror.weather.hourly/rain (s/coll-of ::rain :gen-max 5))
(s/def :smart-mirror.weather.hourly/snowfall (s/coll-of ::snowfall :gen-max 5))
(s/def :smart-mirror.weather.hourly/visibility (s/coll-of ::visibility :gen-max 5))
(s/def :smart-mirror.weather.hourly/uv_index (s/coll-of ::uv_index :gen-max 5))
(s/def :smart-mirror.weather.hourly/sunshine_duration (s/coll-of ::sunshine_duration :gen-max 5))

(s/def ::hourly
  (s/keys :req-un [:smart-mirror.weather.hourly/time
                   :smart-mirror.weather.hourly/temperature_2m
                   :smart-mirror.weather.hourly/relative_humidity_2m
                   :smart-mirror.weather.hourly/apparent_temperature
                   :smart-mirror.weather.hourly/precipitation_probability
                   :smart-mirror.weather.hourly/rain
                   :smart-mirror.weather.hourly/snowfall
                   :smart-mirror.weather.hourly/visibility
                   :smart-mirror.weather.hourly/uv_index
                   :smart-mirror.weather.hourly/sunshine_duration]))

;; daily
(s/def :smart-mirror.weather.daily/daily-interval (s/with-gen string? #(gen/fmap str (date-gen))))
(s/def :smart-mirror.weather.daily/time (s/coll-of :smart-mirror.weather.daily/daily-interval :gen-max 7))
(s/def :smart-mirror.weather.daily/temperature_2m_max (s/coll-of ::temperature_2m :gen-max 7))
(s/def :smart-mirror.weather.daily/temperature_2m_min (s/coll-of ::temperature_2m :gen-max 7))
(s/def :smart-mirror.weather.daily/sunrise (s/coll-of ::hourly-interval :gen-max 7))
(s/def :smart-mirror.weather.daily/sunset (s/coll-of ::hourly-interval :gen-max 7))
(s/def :smart-mirror.weather.daily/daylight_duration (s/coll-of ::daylight_duration :gen-max 5))
(s/def :smart-mirror.weather.daily/rain_sum (s/coll-of ::rain :gen-max 5))
(s/def :smart-mirror.weather.daily/snowfall_sum (s/coll-of ::snowfall :gen-max 5))
(s/def :smart-mirror.weather.daily/precipitation_hours (s/coll-of ::hour_h :gen-max 5))
(s/def :smart-mirror.weather.daily/precipitation_probability_max (s/coll-of ::precipitation_probability :gen-max 5))

(s/def ::daily
  (s/keys :req-un [:smart-mirror.weather.daily/time
                   :smart-mirror.weather.daily/temperature_2m_max
                   :smart-mirror.weather.daily/temperature_2m_min
                   :smart-mirror.weather.daily/sunrise
                   :smart-mirror.weather.daily/sunset
                   :smart-mirror.weather.daily/daylight_duration
                   :smart-mirror.weather.daily/rain_sum
                   :smart-mirror.weather.daily/snowfall_sum
                   :smart-mirror.weather.daily/precipitation_hours
                   :smart-mirror.weather.daily/precipitation_probability_max]))

(s/def ::weather-forecast
  (s/keys :req-un [::latitude
                   ::longitude
                   ::elevation
                   ::current
                   ::hourly
                   ::daily]))
