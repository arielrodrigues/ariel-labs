(ns common.time
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [java-time.api :as jt]))

(defn zoned-date-time-gen []
  (gen/fmap
   (fn [[date time tz]]
     (jt/zoned-date-time (jt/local-date-time date time) tz))
   (gen/tuple
      ;; Generate dates within a specific range (e.g., within 2020-2025)
    (gen/fmap #(jt/local-date %) (gen/choose 2020 2025))
      ;; Generate times (within the day range)
    (gen/fmap #(apply jt/local-time %) (gen/tuple (gen/choose 0 23) (gen/choose 0 59) (gen/choose 0 59)))
      ;; Generate valid time zones (you can pick a subset of zones)
    (gen/elements (jt/available-zone-ids)))))

(s/def ::zoned-date-time (s/with-gen #(instance? java.time.ZonedDateTime %)
                           zoned-date-time-gen))

(defn now
  ([] (jt/zoned-date-time))
  ([& params] (apply jt/zoned-date-time params)))

(defn ->zone-name [zoned-date-time]
  (-> zoned-date-time
      jt/zone-id
      str))

(defn ->short-zone-name [zoned-date-time]
  (jt/format "v" zoned-date-time))

(defn ->timestamp [date-time]
  (-> date-time
      jt/instant
      jt/as-map
      :instant-seconds))

(defn ->offset [zoned-date-time]
  (jt/format "XXX" zoned-date-time))

(defn ->iso-date-time [zoned-date-time]
  (jt/format :iso-date-time zoned-date-time))

(defn weekend? [zoned-date-time]
  (jt/weekend? zoned-date-time))

(defn change-zone
  [zoned-date-time new-zone]
  (jt/zoned-date-time zoned-date-time new-zone))
