(ns smart-mirror.integration.setup
  (:require [smart-mirror.system :as system]
            [state-flow.api :as flow]))

(defn build-initial-state
  []
  {:system (system/create-and-start-system! system/test-system-map)})

(defmacro defflow
  "Starts the test system before running state-flow tests"
  [flow-name & body]
  `(flow/defflow ~flow-name
     ~{:init build-initial-state}
     ~@body))
