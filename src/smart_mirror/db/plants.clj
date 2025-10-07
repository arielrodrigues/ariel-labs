(ns smart-mirror.db.plants
  (:require [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [common.protocols.database :as protocols.database]
            [common.time :as time]
            [smart-mirror.plants :as plants]))

;; Queries

(def find-all-active-plants
  '[:find ?e ?plant-id ?name ?location ?water-freq ?last-watered
    :where
    [?e :plant/active? true]
    [?e :plant/id ?plant-id]
    [?e :plant/name ?name]
    [?e :plant/location ?location]
    [?e :plant/water-frequency-days ?water-freq]
    [(get-else $ ?e :plant/last-watered #inst "1970-01-01") ?last-watered]])

(def find-plant-by-id
  '[:find ?e
    :in $ ?plant-id
    :where
    [?e :plant/id ?plant-id]
    [?e :plant/active? true]])

(def find-plants-due-for-watering
  '[:find ?e ?plant-id ?name ?last-watered ?water-freq
    :in $ ?now-millis
    :where
    [?e :plant/active? true]
    [?e :plant/id ?plant-id]
    [?e :plant/name ?name]
    [?e :plant/water-frequency-days ?water-freq]
    [(get-else $ ?e :plant/last-watered #inst "1970-01-01") ?last-watered]
    [(.getTime ?last-watered) ?last-watered-millis]
    [(- ?now-millis ?last-watered-millis) ?millis-since]
    [(* ?water-freq 24 60 60 1000) ?freq-millis]
    [(>= ?millis-since ?freq-millis)]])

(def find-watering-history-for-plant
  '[:find ?watered-at ?watered-by ?notes ?amount
    :in $ ?plant-entity
    :where
    [?w :watering/plant ?plant-entity]
    [?w :watering/watered-at ?watered-at]
    [(get-else $ ?w :watering/watered-by "") ?watered-by]
    [(get-else $ ?w :watering/notes "") ?notes]
    [(get-else $ ?w :watering/amount-ml 0) ?amount]])

(def find-notifications
  '[:find ?e ?notification-id ?topic ?sent-at ?channel
    :where
    [?e :notification/id ?notification-id]
    [?e :notification/topic ?topic]
    [?e :notification/sent-at ?sent-at]
    [?e :notification/channel ?channel]])

(defn days-between [date1 date2]
  (time/days-between date1 date2))

(defn next-watering-date [last-watered frequency-days]
  (when last-watered
    (time/+days last-watered frequency-days)))

;; DB → Internal model transformations
(defn db-entity->plant
  "Transform database entity to internal plant model"
  [db-entity]
  (when db-entity
    #::plants{:id (:plant/id db-entity)
              :name (:plant/name db-entity)
              :scientific-name (:plant/scientific-name db-entity)
              :pic-url (:plant/pic-url db-entity)
              :water-frequency-days (:plant/water-frequency-days db-entity)
              :last-watered (:plant/last-watered db-entity)
              :notes (:plant/notes db-entity)
              :location (:plant/location db-entity)
              :type (:plant/type db-entity)
              :active? (:plant/active? db-entity)
              :next-watering (when (:plant/last-watered db-entity)
                               (next-watering-date (time/->local-date-time (:plant/last-watered db-entity))
                                                   (:plant/water-frequency-days db-entity)))}))

(defn db-results->plants
  "Transform database query results to internal plant models with computed fields"
  [results]
  (map (fn [[entity-id plant-id name location water-freq last-watered]]
         (let [actual-last-watered (when (and last-watered
                                              (not= last-watered #inst "1970-01-01"))
                                     last-watered)]
           #::plants{:entity-id entity-id
                     :id plant-id
                     :name name
                     :location location
                     :water-frequency-days water-freq
                     :last-watered actual-last-watered
                     :next-watering (next-watering-date actual-last-watered water-freq)}))
       results))

(defn db-watering-results->waterings
  "Transform watering query results to internal watering models"
  [results]
  (map (fn [[watered-at watered-by notes amount]]
         #::plants{:watered-at watered-at
                   :watered-by (when (not= watered-by "") watered-by)
                   :watering-notes (when (not= notes "") notes)
                   :amount-ml (when (not= amount 0) amount)})
       results))

(defn db-results->notifications
  [results]
  (map (fn [[_entity-id notification-id topic sent-at channel]]
         {:id notification-id
          :topic topic
          :sent-at sent-at
          :channel channel})
       results))

(s/fdef create-plant!
  :args (s/cat :database any? :plant-data ::plants/plant-input)
  :ret uuid?)
(defn create-plant! [database plant-data]
  (let [plant-id (random-uuid)
        base-data {:plant/id plant-id
                   :plant/name (::plants/name plant-data)
                   :plant/water-frequency-days (::plants/water-frequency-days plant-data)
                   :plant/location (::plants/location plant-data)
                   :plant/type (::plants/type plant-data :unknown)
                   :plant/active? true}
        ;; Only include non-nil optional fields
        optional-data (cond-> {}
                        (::plants/scientific-name plant-data)
                        (assoc :plant/scientific-name (::plants/scientific-name plant-data))

                        (::plants/pic-url plant-data)
                        (assoc :plant/pic-url (::plants/pic-url plant-data))

                        (::plants/notes plant-data)
                        (assoc :plant/notes (::plants/notes plant-data)))
        tx-data [(merge base-data optional-data)]]
    (protocols.database/transact database tx-data)
    plant-id))

(s/fdef update-plant!
  :args (s/cat :database any? :plant-id uuid? :updates ::plants/plant-updates)
  :ret (s/nilable uuid?))
 (defn update-plant! [database plant-id updates]
   (when-let [entity-id (ffirst (protocols.database/query database find-plant-by-id plant-id))]
     (let [db-updates (set/rename-keys updates {::plants/name :plant/name
                                                ::plants/scientific-name :plant/scientific-name
                                                ::plants/pic-url :plant/pic-url
                                                ::plants/water-frequency-days :plant/water-frequency-days
                                                ::plants/notes :plant/notes
                                                ::plants/location :plant/location
                                                ::plants/type :plant/type})
           tx-data (reduce-kv (fn [acc k v]
                                (conj acc [:db/add entity-id k v]))
                              []
                              db-updates)]
       (protocols.database/transact database tx-data)
       plant-id)))

(s/fdef delete-plant!
  :args (s/cat :database any? :plant-id uuid?)
  :ret (s/nilable uuid?))
(defn delete-plant! [database plant-id]
  (when-let [entity-id (ffirst (protocols.database/query database find-plant-by-id plant-id))]
    (protocols.database/transact database [[:db/add entity-id :plant/active? false]])
    plant-id))

(s/fdef get-plant
  :args (s/cat :database any? :plant-id uuid?)
  :ret (s/nilable ::plants/plant))
(defn get-plant [database plant-id]
  (when-let [entity-id (ffirst (protocols.database/query database find-plant-by-id plant-id))]
    (let [db-entity (protocols.database/pull database '[*] entity-id)]
      (db-entity->plant db-entity))))

(s/fdef get-all-plants
  :args (s/cat :database any?)
  :ret ::plants/plants)
(defn get-all-plants [database]
  (let [results (protocols.database/query database find-all-active-plants)]
    (db-results->plants results)))

(s/fdef get-plants-due-today
  :args (s/cat :database any? :as-of ::time/zoned-date-time)
  :ret ::plants/plants)
(defn get-plants-due-today [database as-of]
  (let [millis-now (time/->millis as-of)
        results (protocols.database/query database find-plants-due-for-watering millis-now)]
    (map (fn [[entity-id plant-id name last-watered water-freq]]
           #::plants{:entity-id entity-id
                     :id plant-id
                     :name name
                     :last-watered last-watered
                     :water-frequency-days water-freq
                     :days-overdue (when last-watered
                                     (- (days-between last-watered as-of) water-freq))})
         results)))

(s/fdef record-watering!
  :args (s/cat :database any? :plant-id uuid? :watering-data ::plants/watering-input)
  :ret (s/nilable uuid?))
(defn record-watering! [database plant-id watering-data]
  (when-let [plant-entity-id (ffirst (protocols.database/query database find-plant-by-id plant-id))]
    (let [watering-id (random-uuid)
          now (time/now-as-date)
          base-watering {:watering/id watering-id
                         :watering/plant plant-entity-id
                         :watering/watered-at (:watered-at watering-data now)
                         :db/id "new-watering"}
          ;; Only include non-nil optional fields
          optional-watering (cond-> {}
                              (::plants/watered-by watering-data)
                              (assoc :watering/watered-by (::plants/watered-by watering-data))

                              (::plants/watering-notes watering-data)
                              (assoc :watering/notes (::plants/watering-notes watering-data))

                              (::plants/amount-ml watering-data)
                              (assoc :watering/amount-ml (::plants/amount-ml watering-data)))
          tx-data [(merge base-watering optional-watering)
                   [:db/add plant-entity-id :plant/last-watered now]]]
      (protocols.database/transact database tx-data)
      watering-id)))

(s/fdef get-watering-history
  :args (s/cat :database any? :plant-id uuid?)
  :ret ::plants/waterings)
(defn get-watering-history [database plant-id]
  (when-let [plant-entity-id (ffirst (protocols.database/query database find-plant-by-id plant-id))]
    (let [results (protocols.database/query database find-watering-history-for-plant plant-entity-id)]
      (db-watering-results->waterings results))))

(s/fdef register-notification!
  :args (s/cat :database any? :as-of ::time/zoned-date-time)
  :ret uuid?)
(defn register-notification! [database as-of]
  (let [notification-id (random-uuid)
        base-data {:notification/id notification-id
                   :notification/topic :notification/water-plants
                   :notification/sent-at (time/->java-date as-of)
                   :notification/channel :notification/push}
        tx-data [base-data]]
    (protocols.database/transact database tx-data)
    notification-id))

(defn get-notifications [database]
  (-> database
      (protocols.database/query find-notifications)
      db-results->notifications))
