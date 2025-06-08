(ns smart-mirror.calendar
  (:require [clojure.spec.alpha :as s]))

(s/def ::owner string?) ;; email
(s/def ::summary string?)
(s/def ::description string?)
(s/def ::status string?)

;; Start and end can contain dateTime or date (depending on all-day vs timed events)
(s/def ::date string?)       ;; e.g., "2025-06-03"
(s/def ::date-time string?)   ;; e.g., "2025-06-03T10:00:00Z"
(s/def ::time-zone string?)

(s/def ::event-time
  (s/keys :req [::date ::date-time ::time-zone]))

(s/def ::start ::event-time)
(s/def ::end ::event-time)

(s/def ::event
  (s/keys :req [::summary
                ::description
                ::start
                ::end
                ::status]))

(s/def ::events (s/coll-of ::event))

(s/def ::calendar
  (s/keys :req [::owner
                ::events]))
