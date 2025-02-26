(ns smart-mirror.integration.setup
  (:require [clojure.test.check :refer [quick-check]]
            [clojure.test.check.properties :as prop]
            [smart-mirror.system :as system]
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

(defmacro defflow*
  [flow-name _options & props]
  `(flow/defflow ~flow-name
     ~{:init build-initial-state}
     (flow/return (quick-check 100 (prop/for-all ~@props)))))


#_(defflow ariel {:run asdadasd}
    {:num-tests 10}
    [a (s/gen x)
     b (s/gen y)]
    (flow "ariel"
          []
          (flow "ariel 2"
                (match?))))
