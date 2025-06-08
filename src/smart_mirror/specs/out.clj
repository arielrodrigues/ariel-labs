(ns smart-mirror.specs.out
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [smart-mirror.weather :as weather]))

;; weather
(s/def ::latitude (s/with-gen float? #(gen/double* {:min -90 :max 90})))
(s/def ::longitude (s/with-gen float? #(gen/double* {:min -180 :max 180})))
(s/def ::elevation (s/with-gen float? #(gen/double* {:min 0 :max 10000})))

(s/def ::hourly-interval (s/with-gen string?
                           (fn [] (let [date (weather/date-gen)
                                        hour (weather/hour-gen)]
                                    (gen/return (str (gen/generate date) "T" (gen/generate hour)))))))

(s/def ::temperature (s/with-gen float? #(gen/double* {:min -52 :max 42 :NaN? false})))
(s/def ::relative-humidity (s/with-gen integer? #(gen/large-integer* {:min 5 :max 100})))
(s/def ::apparent-temperature (s/with-gen float? #(gen/double* {:min -52 :max 42 :NaN? false})))
(s/def ::precipitation (s/with-gen float? #(gen/double* {:min 0 :max 100 :NaN? false})))
(s/def ::precipitation-probability (s/with-gen integer? #(gen/choose 0 100)))
(s/def ::rain (s/with-gen float? #(gen/double* {:min 0 :max 100 :NaN? false})))
(s/def ::snowfall (s/with-gen float? #(gen/double* {:min 0 :max 30 :NaN? false})))
(s/def ::wind-speed (s/with-gen float? #(gen/double* {:min 5 :max 60 :NaN? false})))
(s/def ::visibility (s/with-gen float? #(gen/double* {:min 10000 :max 50000 :NaN? false})))
(s/def ::uv-index (s/with-gen float? #(gen/double* {:min 0 :max 10 :NaN? false})))
(s/def ::sunshine-duration (s/with-gen float? #(gen/double* {:min 1 :max 14 :NaN? false})))
(s/def ::daylight-duration (s/with-gen float? #(gen/double* {:min 10000 :max 60000 :NaN? false})))
(s/def ::hour (s/with-gen float? #(gen/fmap float (gen/choose 0 23))))
(s/def ::weather-code (s/with-gen integer? #(gen/choose 0 99)))

(s/def ::current (s/keys :req-un [::temperature
                                  ::relative-humidity
                                  ::apparent-temperature
                                  ::precipitation
                                  ::rain
                                  ::snowfall
                                  ::wind-speed
                                  ::weather-code]))

;; hourly
(s/def :smart-mirror.weather.hourly/time (s/coll-of ::hourly-interval :gen-max 5))
(s/def :smart-mirror.weather.hourly/temperature (s/coll-of ::temperature :gen-max 5))
(s/def :smart-mirror.weather.hourly/relative-humidity (s/coll-of ::relative-humidity :gen-max 5))
(s/def :smart-mirror.weather.hourly/apparent-temperature (s/coll-of ::apparent-temperature :gen-max 5))
(s/def :smart-mirror.weather.hourly/precipitation-probability (s/coll-of ::precipitation-probability :gen-max 5))
(s/def :smart-mirror.weather.hourly/rain (s/coll-of ::rain :gen-max 5))
(s/def :smart-mirror.weather.hourly/snowfall (s/coll-of ::snowfall :gen-max 5))
(s/def :smart-mirror.weather.hourly/visibility (s/coll-of ::visibility :gen-max 5))
(s/def :smart-mirror.weather.hourly/uv-index (s/coll-of ::uv-index :gen-max 5))
(s/def :smart-mirror.weather.hourly/sunshine-duration (s/coll-of ::sunshine-duration :gen-max 5))

(s/def ::hourly
  (s/keys :req-un [:smart-mirror.weather.hourly/time
                   :smart-mirror.weather.hourly/temperature
                   :smart-mirror.weather.hourly/relative-humidity
                   :smart-mirror.weather.hourly/apparent-temperature
                   :smart-mirror.weather.hourly/precipitation-probability
                   :smart-mirror.weather.hourly/rain
                   :smart-mirror.weather.hourly/snowfall
                   :smart-mirror.weather.hourly/visibility
                   :smart-mirror.weather.hourly/uv-index
                   :smart-mirror.weather.hourly/sunshine-duration]))

;; daily
(s/def :smart-mirror.weather.daily/daily-interval (s/with-gen string? #(gen/fmap str (weather/date-gen))))
(s/def :smart-mirror.weather.daily/time (s/coll-of :smart-mirror.weather.daily/daily-interval :gen-max 7))
(s/def :smart-mirror.weather.daily/temperature-max (s/coll-of ::temperature :gen-max 7))
(s/def :smart-mirror.weather.daily/temperature-min (s/coll-of ::temperature :gen-max 7))
(s/def :smart-mirror.weather.daily/sunrise (s/coll-of ::hourly-interval :gen-max 7))
(s/def :smart-mirror.weather.daily/sunset (s/coll-of ::hourly-interval :gen-max 7))
(s/def :smart-mirror.weather.daily/daylight-duration (s/coll-of ::daylight-duration :gen-max 5))
(s/def :smart-mirror.weather.daily/rain-sum (s/coll-of ::rain :gen-max 5))
(s/def :smart-mirror.weather.daily/snowfall-sum (s/coll-of ::snowfall :gen-max 5))
(s/def :smart-mirror.weather.daily/precipitation-hours (s/coll-of ::hour :gen-max 5))
(s/def :smart-mirror.weather.daily/precipitation-probability-max (s/coll-of ::precipitation-probability :gen-max 5))
(s/def :smart-mirror.weather.daily/weather-code (s/coll-of ::weather-code))

(s/def ::daily
  (s/keys :req-un [:smart-mirror.weather.daily/time
                   :smart-mirror.weather.daily/temperature-max
                   :smart-mirror.weather.daily/temperature-min
                   :smart-mirror.weather.daily/sunrise
                   :smart-mirror.weather.daily/sunset
                   :smart-mirror.weather.daily/daylight-duration
                   :smart-mirror.weather.daily/rain-sum
                   :smart-mirror.weather.daily/snowfall-sum
                   :smart-mirror.weather.daily/precipitation-hours
                   :smart-mirror.weather.daily/precipitation-probability-max
                   :smart-mirror.weather.daily/weather-code]))

(s/def ::weather-forecast
  (s/keys :req-un [::latitude
                   ::longitude
                   ::elevation
                   ::current
                   ::hourly
                   ::daily
                   ::weather/default-units]))
