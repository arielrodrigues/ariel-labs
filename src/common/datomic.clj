(ns common.datomic
  (:require [com.stuartsierra.component :as component]
            [common.protocols.config :as protocols.config]
            [common.protocols.database :as protocols.database]
            [datomic.client.api :as d]))

(defn- check-fault [database operation]
  (when-let [*faults* (:*faults* database)]
    (get @*faults* operation)))

(defn- throw-database-fault [fault]
  (case (:type fault)
    :connection-timeout
    (throw (ex-info "Database connection timeout"
                    {:cognitect.anomalies/category :cognitect.anomalies/unavailable}))

    :transaction-conflict
    (throw (ex-info "Transaction conflict detected"
                    {:cognitect.anomalies/category :cognitect.anomalies/conflict}))

    :query-timeout
    (throw (ex-info "Query execution timeout"
                    {:cognitect.anomalies/category :cognitect.anomalies/interrupted}))))

(defrecord DatomicDatabase [config client connection]
  component/Lifecycle
  (start [this]
    (let [db-name (protocols.config/read-value config :database-name)
          client (d/client {:server-type :dev-local
                           :storage-dir :mem
                           :system "datomic-samples"})]
      (d/create-database client {:db-name db-name})
      (let [conn (d/connect client {:db-name db-name})]
        (assoc this :client client :connection conn))))

  (stop [this]
    (assoc this :client nil :connection nil))

  protocols.database/Database
  (get-connection [this]
    connection)

  (transact [this tx-data]
    (d/transact connection {:tx-data tx-data}))

  (query [this query]
    (d/q {:query query :args [(d/db connection)]}))

  (query [this query arg1]
    (d/q {:query query :args [(d/db connection) arg1]}))

  (query [this query arg1 arg2]
    (d/q {:query query :args [(d/db connection) arg1 arg2]}))

  (entity [this entity-id]
    (d/pull (d/db connection) '[*] entity-id))

  (pull [this pattern entity-id]
    (d/pull (d/db connection) pattern entity-id)))

(defrecord MockDatabase [config client connection db-name *faults*]
  component/Lifecycle
  (start [this]
    ;; Create a unique in-memory database for each test run
    (let [db-name (str "test-db-" (random-uuid))
          client (d/client {:server-type :dev-local
                            :storage-dir :mem
                            :system "test-datomic"})]
      (println "MockDatabase START: Creating database" db-name)
      (d/create-database client {:db-name db-name})
      (let [conn (d/connect client {:db-name db-name})]
        (assoc this :client client :connection conn :db-name db-name :*faults* (atom {})))))

  (stop [this]
    ;; Clean up by deleting the test database
    (when (and client db-name)
      (try
        (println "MockDatabase STOP: Deleting database" db-name)
        (d/delete-database client {:db-name db-name})
        (println "MockDatabase STOP: Successfully deleted database" db-name)
        (catch Exception e
          (println "MockDatabase STOP: Error deleting database" db-name ":" (.getMessage e))
          ;; Ignore cleanup errors
          nil)))
    (assoc this :client nil :connection nil :db-name nil :*faults* nil))

  protocols.database/Database
  (get-connection [this]
    connection)

  (transact [this tx-data]
    (if-let [fault (check-fault this :transact)]
      (throw-database-fault fault)
      (d/transact connection {:tx-data tx-data})))

  (query [this query]
    (if-let [fault (check-fault this :query)]
      (throw-database-fault fault)
      (d/q {:query query :args [(d/db connection)]})))

  (query [this query arg1]
    (if-let [fault (check-fault this :query)]
      (throw-database-fault fault)
      (d/q {:query query :args [(d/db connection) arg1]})))

  (query [this query arg1 arg2]
    (if-let [fault (check-fault this :query)]
      (throw-database-fault fault)
      (d/q {:query query :args [(d/db connection) arg1 arg2]})))

  (entity [this entity-id]
    (if-let [fault (check-fault this :entity)]
      (throw-database-fault fault)
      (d/pull (d/db connection) '[*] entity-id)))

  (pull [this pattern entity-id]
    (if-let [fault (check-fault this :pull)]
      (throw-database-fault fault)
      (d/pull (d/db connection) pattern entity-id))))

(defn new-datomic-database []
  (component/using
   (map->DatomicDatabase {})
   [:config]))

(defn new-mock-database []
  (component/using
   (map->MockDatabase {})
   [:config]))
