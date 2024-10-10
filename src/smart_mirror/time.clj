(ns smart-mirror.time
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [common.time :as time]
            [java-time.api :as jt]))

(def timezones (set (jt/available-zone-ids)))
(s/def ::valid-timezone timezones)
(s/def ::valid-timezones (s/coll-of ::valid-timezone))

(s/def ::utc-offset string?)
(s/def ::name timezones)
(s/def ::abbreviation string?)
(s/def ::timezone (s/keys :req-un [::name ::abbreviation]))
(s/def ::date-time string?)
(s/def ::timestamp number?)
(s/def ::weekend? boolean?)

(s/def ::time
  (s/keys :req [::utc-offset
                ::timezone
                ::date-time
                ::timestamp
                ::weekend?]))

(s/fdef valid-timezone?
  :args (s/cat :timezone string?)
  :ret boolean?)
(defn valid-timezone? [timezone]
  (boolean (some #{timezone} timezones)))

(def valid-timezones? (partial every? valid-timezone?))

(s/fdef ->time
  :args (s/cat :as-of ::time/zoned-date-time)
  :ret ::time)
(defn ->time [as-of]
  #::{:utc-offset (time/->offset as-of)
       :timezone {:name (time/->zone-name as-of)
                  :abbreviation (time/->short-zone-name as-of)}
       :date-time (time/->iso-date-time as-of)
       :timestamp (time/->timestamp as-of)
       :weekend? (time/weekend? as-of)})

(s/fdef time+zones
  :args (s/cat :base ::time/zoned-date-time :zones ::valid-timezone)
  :ret (s/coll-of ::time))
(defn time+zones [base zones]
  (if (empty? zones)
    [(->time base)]
    (->> zones
         (map #(time/change-zone base %))
         (cons base)
         (map ->time))))

(s/fdef qs->timezones
  :args (s/cat :qs string?)
  :ret (s/nilable ::valid-timezones))
 (defn qs->timezones
   [qs]
   (let [timezones (if (empty? qs) [] (string/split qs #" "))]
     (when (valid-timezones? timezones)
       timezones)))
