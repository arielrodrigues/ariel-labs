(ns common.protocols.database)

(defprotocol Database
  (get-connection [this] "Get database connection")
  (transact [this tx-data] "Execute transaction")
  (query [this query] [this query arg1] [this query arg1 arg2] "Execute query")
  (entity [this entity-id] "Get entity by ID")
  (pull [this pattern entity-id] "Pull specific attributes from entity"))