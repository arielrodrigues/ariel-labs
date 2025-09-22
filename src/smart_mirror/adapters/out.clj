(ns smart-mirror.adapters.out
  (:require [clojure.spec.alpha :as s]
            [smart-mirror.calendar :as calendar]
            [smart-mirror.specs.out :as out]
            [smart-mirror.weather :as weather]))

(defn- ->unamespaced
  [m]
  (update-keys m (comp keyword name)))

(s/fdef weather-forecast->wire
  :args (s/cat :forecast ::weather/forecast)
  :ret  ::out/weather-forecast)
(defn weather-forecast->wire
  [{::weather/keys [latitude longitude elevation current hourly daily]}]
  {:latitude latitude
   :longitude longitude
   :elevation elevation
   :current (->unamespaced current)
   :hourly (->unamespaced hourly)
   :daily (->unamespaced daily)
   :default-units weather/default-units})

(defn- event-time->wire
  [event-time]
  (if (vector? event-time)
    (let [[_tag event-time-data] event-time]
      (cond-> {}
        (:date event-time-data) (assoc :date (:date event-time-data))
        (:dateTime event-time-data) (assoc :date-time (:dateTime event-time-data))
        (:timeZone event-time-data) (assoc :time-zone (:timeZone event-time-data))))
    (let [{::calendar/keys [date date-time time-zone]} event-time]
      (cond-> {}
        date (assoc :date date)
        date-time (assoc :date-time date-time)
        time-zone (assoc :time-zone time-zone)))))

(defn- event->wire
  [{::calendar/keys [summary description status start end]}]
  (cond-> {:summary summary
           :status status
           :start (event-time->wire start)
           :end (event-time->wire end)}
    description (assoc :description description)))

(s/fdef calendar->wire
  :args (s/cat :calendar ::calendar/calendar)
  :ret ::out/calendar)
(defn calendar->wire
  [{::calendar/keys [owner events]}]
  (cond-> {:events (map event->wire events)}
    owner (assoc :owner owner)))
