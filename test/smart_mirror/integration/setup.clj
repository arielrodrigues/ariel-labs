(ns smart-mirror.integration.setup
  (:require [clojure.test]
            [clojure.test.check :as tc]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [smart-mirror.system :as system]
            [state-flow.api :as flow]
            [state-flow.cljtest]))

(defn build-initial-state
  []
  {:system (system/create-and-start-system! system/test-system-map)})

(defmacro defflow
  "Starts the test system before running state-flow tests"
  [flow-name & body]
  `(flow/defflow ~flow-name
     ~{:init build-initial-state}
     ~@body))

(defmacro defflow-quickcheck
  [test-name bindings & flow-body]
  `(clojure.test/deftest ~test-name
     (let [property# (clojure.test.check.properties/for-all
                      ~bindings
                      (try
                        (let [[ret# state#] (state-flow.api/run* {:init build-initial-state}
                                                                 (state-flow.api/flow ~(str test-name) ~@flow-body))
                              assertions#   (get-in (meta state#) [:test-report :assertions])
                              all-passed?#  (every? #(= :pass (:type %)) assertions#)]
                          (doseq [assertion-data# assertions#]
                            (clojure.test/report (#'state-flow.cljtest/clojure-test-report assertion-data#)))
                          all-passed?#)
                        (catch Throwable t#
                          (clojure.test/do-report
                           {:type     :error
                            :message  (str "Exception in property-based state-flow: " (.getMessage t#))
                            :expected true
                            :actual   t#})
                          false)))]
       (tc/quick-check 100 property#))))
