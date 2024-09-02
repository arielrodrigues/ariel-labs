(ns common.routes-test
  (:require [common.routes]
            [clojure.test :refer [deftest testing is]]
            [clojure.test.check.generators :as gen]))

(defn gen-valid-http-method (-> common.routes/valid-http-methods gen/elements gen/generate))
(defn gen-valid-)
(def default-handler identity)

(def valid-route-map
  ["/resource" {(valid-http-method) {:handler default-handler}
                (valid-http-method) {:handler default-handler}}
   "/version" {(valid-http-method) {:handler default-handler}}])

(deftest expand-route-map!-test
  (testing "Given a route map. When it is valid. It must return ?"
    (is (= (common.routes/expand-route-map! valid-route-map)
           {}))))
