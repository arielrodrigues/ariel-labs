(ns smart-mirror.adapters.out
  (:require [clojure.spec.alpha :as s]
            [medley.core :refer [assoc-some]]
            [smart-mirror.calendar :as calendar]
            [smart-mirror.plants :as plants]
            [smart-mirror.specs.out :as out]
            [smart-mirror.time :as time]
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

(defn- time->wire
  [{::time/keys [utc-offset timezone date-time timestamp weekend?]}]
  {:utc-offset utc-offset
   :timezone {:name (:name timezone)
              :abbreviation (:abbreviation timezone)}
   :date-time date-time
   :timestamp timestamp
   :weekend? weekend?})

(s/fdef times->wire
  :args (s/cat :times (s/coll-of ::time/time))
  :ret ::out/times)
(defn times->wire
  [times]
  (map time->wire times))

;; Plants
(defn- instant->iso-string [inst] (when inst (.toString inst)))
(defn- keyword->string [kw] (when kw (name kw)))
(defn- uuid->string [uuid] (when uuid (str uuid)))

(s/fdef plant->wire
  :args (s/cat :plant ::plants/plant)
  :ret ::out/plant-response)
(defn plant->wire
  [{::plants/keys [id name scientific-name pic-url water-frequency-days
                   last-watered notes location type
                   next-watering days-overdue]}]
  (assoc-some {:id (uuid->string id)
               :name name
               :water-frequency-days water-frequency-days
               :location location}
              :scientific-name scientific-name
              :pic-url pic-url
              :last-watered (instant->iso-string last-watered)
              :notes notes
              :type (keyword->string type)
              :next-watering (instant->iso-string next-watering)
              :days-overdue days-overdue))

(s/fdef plants->wire
  :args (s/cat :plants ::plants/plants)
  :ret ::out/plants-response)
(defn plants->wire [plants] (map plant->wire plants))

(s/fdef watering->wire
  :args (s/cat :watering ::plants/watering)
  :ret ::out/watering-response)
(defn watering->wire
  [{::plants/keys [watering-id plant-id watered-at watered-by
                   watering-notes amount-ml]}]
  (assoc-some {:watering-id (uuid->string watering-id)
               :plant-id (uuid->string plant-id)
               :watered-at (instant->iso-string watered-at)}
              :watered-by watered-by
              :watering-notes watering-notes
              :amount-ml amount-ml))

(s/fdef waterings->wire
  :args (s/cat :waterings ::plants/waterings)
  :ret ::out/waterings-response)
(defn waterings->wire [waterings] (map watering->wire waterings))
