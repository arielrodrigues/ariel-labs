(ns smart-mirror.time
  (:require [clojure.spec.alpha :as s]
            [common.time :as time]
            [java-time.api :as jt]))

(def timezones (set (jt/available-zone-ids)))

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
