(ns smart-mirror.specs.in
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [common.time]
            [smart-mirror.coordinates :as coordinates]
            [smart-mirror.time :as time]))

;; ipinfo
(s/def ::city string?)
(s/def ::region string?)
(s/def ::country string?)
(s/def ::loc (s/spec ::coordinates/str-coordinates))
(s/def ::timezone ::time/name)

(s/def ::ip-info
  (s/keys :req-un [::city
                   ::region
                   ::country
                   ::loc
                   ::timezone]))

;; weather

(s/def ::latitude (s/with-gen float? #(gen/double* {:min -90 :max 90})))
(s/def ::longitude (s/with-gen float? #(gen/double* {:min -180 :max 180})))
(s/def ::elevation (s/with-gen float? #(gen/double* {:min 0 :max 10000})))

(defn date-gen [] (gen/fmap common.time/->local-date (gen/tuple (gen/choose 2018 2028) (gen/choose 1 12) (gen/choose 1 28))))
(defn hour-gen [] (gen/fmap common.time/->local-time (gen/tuple (gen/choose 0 23))))
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
(s/def ::weather_code (s/with-gen integer? #(gen/choose 0 99)))

(s/def ::current (s/keys :req-un [::interval
                                  ::temperature_2m
                                  ::relative_humidity_2m
                                  ::apparent_temperature
                                  ::precipitation
                                  ::rain
                                  ::snowfall
                                  ::wind_speed_10m
                                  ::weather_code]))

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
(s/def :smart-mirror.weather.daily/weather_code (s/coll-of ::weather_code))

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
                   :smart-mirror.weather.daily/precipitation_probability_max
                   :smart-mirror.weather.daily/weather_code]))

(s/def ::weather-forecast
  (s/keys :req-un [::latitude
                   ::longitude
                   ::elevation
                   ::current
                   ::hourly
                   ::daily]))

;; calendar
(s/def ::summary (s/with-gen string? #(gen/elements ["Meeting" "Appointment" "Call" "Event"])))
(s/def ::description (s/with-gen (s/nilable string?) #(gen/frequency [[1 (gen/return nil)] [1 (gen/return "Important meeting")]])))
(s/def ::status (s/with-gen string? #(gen/elements ["accepted" "tentative" "declined"])))

;; Start and end can contain dateTime or date (depending on all-day vs timed events)
(s/def ::date (s/with-gen string? #(gen/fmap (fn [[month day]] (format "2025-%02d-%02d" month day)) (gen/tuple (gen/choose 1 12) (gen/choose 1 28)))))
(s/def ::dateTime (s/with-gen string? #(gen/fmap (fn [[month day hour]] (format "2025-%02d-%02dT%02d:00:00Z" month day hour)) (gen/tuple (gen/choose 1 12) (gen/choose 1 28) (gen/choose 9 17)))))
(s/def ::timeZone (s/with-gen string? #(gen/elements ["UTC" "America/New_York" "Europe/London"])))

(s/def ::all-day-event-time
  (s/with-gen (s/keys :req-un [::date])
    #(gen/fmap (fn [[month day]] {:date (format "2025-%02d-%02d" month day)})
               (gen/tuple (gen/choose 1 12) (gen/choose 1 28)))))

(s/def ::timed-event-time
  (s/with-gen (s/keys :req-un [::dateTime] :opt-un [::timeZone])
    #(gen/fmap (fn [[month day hour tz]] {:dateTime (format "2025-%02d-%02dT%02d:00:00Z" month day hour) :timeZone tz})
               (gen/tuple (gen/choose 1 12) (gen/choose 1 28) (gen/choose 9 17) (gen/elements ["UTC" "America/New_York" "Europe/London"])))))

(s/def ::event-time (s/or :all-day ::all-day-event-time
                          :timed ::timed-event-time))

(s/def ::start ::event-time)
(s/def ::end ::event-time)

(s/def ::event
  (s/keys :req-un [::summary
                   ::start
                   ::end
                   ::status]
          :opt-un [::description]))

(s/def ::items (s/coll-of ::event :min-count 1 :gen-max 3))

(s/def ::calendar
  (s/keys :req-un [::items]
          :opt-un [::summary]))
