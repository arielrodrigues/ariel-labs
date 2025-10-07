(ns smart-mirror.db.schema
  (:require [datomic.client.api :as d]))

(def plant-schema
  [;; Plant entity
   {:db/ident :plant/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "Unique identifier for a plant"}

   {:db/ident :plant/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Common name of the plant"}

   {:db/ident :plant/scientific-name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Scientific name of the plant"}

   {:db/ident :plant/pic-url
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "URL to plant image"}

   {:db/ident :plant/water-frequency-days
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Number of days between waterings"}

   {:db/ident :plant/last-watered
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Timestamp of last watering"}

   {:db/ident :plant/notes
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Care notes for the plant"}

   {:db/ident :plant/location
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Physical location of the plant"}

   {:db/ident :plant/type
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "Type of plant: :succulent, :tropical, :herb, etc."}

   {:db/ident :plant/active?
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc "Whether the plant is still active (soft delete)"}

   ;; Watering history entity
   {:db/ident :watering/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "Unique identifier for a watering event"}

   {:db/ident :watering/plant
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Reference to the plant that was watered"}

   {:db/ident :watering/watered-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Timestamp when watering occurred"}

   {:db/ident :watering/watered-by
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Person who watered the plant"}

   {:db/ident :watering/notes
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Notes about the watering event"}

   {:db/ident :watering/amount-ml
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Amount of water in milliliters"}

   ;; notifications
   {:db/ident :notification/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "Unique identifier for notifications"}

   {:db/ident :notification/topic
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "Notification topic: :water-plants"}

   {:db/ident :notification/sent-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Timestamp when the notification was sent"}

   {:db/ident :notification/channel
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "Notification channel: :push"}])

(defn install-schema [connection]
  (d/transact connection {:tx-data plant-schema}))
