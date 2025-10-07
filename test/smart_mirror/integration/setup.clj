(ns smart-mirror.integration.setup
  (:require [clojure.test]
            [clojure.test.check :as tc]
            [clojure.test.check.properties :as prop]
            [common.test :as common-test]
            [smart-mirror.system :as system]
            [state-flow.api :as flow]
            [state-flow.cljtest]))

(defn build-initial-state
  ([]
   (build-initial-state (common-test/get-test-seed)))
  ([seed]
   {:system (system/create-and-start-system! system/test-system-map)
    :test-seed seed}))

(defmacro defflow
  "Starts the test system before running state-flow tests"
  [flow-name & body]
  `(flow/defflow ~flow-name
     ~{:init build-initial-state}
     ~@body))

(defmacro defflow-quickcheck
  [test-name options bindings & flow-body]
  (let [default-num-tests 100]
    `(clojure.test/deftest ~test-name
       (let [opts# ~options
             test-seed# (or (:seed opts#) (common-test/get-test-seed))
             num-tests# (or (:num-tests opts#) ~default-num-tests)
             iteration-counter# (atom 0)
             _# (println "Seed:" test-seed#)
             property# (clojure.test.check.properties/for-all
                        ~bindings
                        (try
                          (let [iteration# (swap! iteration-counter# inc)
                                iteration-seed# (+ test-seed# iteration#)
                                [ret# state#] (state-flow.api/run* {:init #(build-initial-state iteration-seed#)}
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
         (tc/quick-check num-tests# property# :seed test-seed#)))))
