(ns common.routes-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [common.routes :as common-routes]))

 (defspec expand-routes!-test 50
   (prop/for-all [routes (s/gen ::common-routes/routes)]
                 (let [routes-vec (vec routes)
                       route-def (if (seq routes-vec)
                                   (subvec routes-vec 0 2)
                                   [])
                       route-path (first route-def)
                       route-methods (second route-def)
                       expanded-route-def (common-routes/expand-routes! route-def)]

                   (testing "Given a well-formed routes collection. When expanded, Then the spec is respected"
                     (is (s/valid? ::common-routes/expanded-routes (common-routes/expand-routes! routes))))

                   (testing "Given an expanded-route, their :path matches the non-expanded form"
                     (is (every? #(= (str "/api" route-path) %) (map :path expanded-route-def))))

                   (testing "Given an expanded-route, all methods are equivalent to those in the non-expanded form"
                     (is (= (set (or (keys route-methods) [])) (set (map :method expanded-route-def))))))))
