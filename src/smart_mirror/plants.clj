(ns smart-mirror.plants
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string]))

;; Plant entity (business logic representation)
(s/def ::id uuid?)
(s/def ::name (s/and string? #(not (clojure.string/blank? %))))
(s/def ::scientific-name (s/nilable string?))
(s/def ::pic-url (s/nilable string?))
(s/def ::water-frequency-days pos-int?)
(s/def ::last-watered (s/nilable inst?))
(s/def ::notes (s/nilable string?))
(s/def ::location (s/and string? #(not (clojure.string/blank? %))))
(s/def ::type #{:succulent :tropical :herb :flowering :fern :cactus :unknown})
(s/def ::active? boolean?)
(s/def ::next-watering (s/nilable inst?))
(s/def ::days-overdue (s/nilable number?))

(s/def ::plant
  (s/keys :req [::id ::name ::water-frequency-days ::location ::active?]
          :opt [::scientific-name ::pic-url ::last-watered ::notes
                ::type ::next-watering ::days-overdue]))

;; Watering entity
(s/def ::watering-id uuid?)
(s/def ::plant-id uuid?)
(s/def ::watered-at inst?)
(s/def ::watered-by (s/nilable string?))
(s/def ::watering-notes (s/nilable string?))
(s/def ::amount-ml (s/nilable pos-int?))

(s/def ::watering
  (s/keys :req [::watering-id ::plant-id ::watered-at]
          :opt [::watered-by ::watering-notes ::amount-ml]))

;; Collections
(s/def ::plants (s/coll-of ::plant))
(s/def ::waterings (s/coll-of ::watering))

;; Input specs for adapters
(s/def ::plant-input
  (s/keys :req [::name ::water-frequency-days ::location]
          :opt [::scientific-name ::pic-url ::notes ::type]))

(s/def ::plant-updates
  (s/keys :opt [::name ::scientific-name ::pic-url ::water-frequency-days
                ::notes ::location ::type]))

(s/def ::watering-input
  (s/keys :opt [::watered-by ::watering-notes ::amount-ml]))