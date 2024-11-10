(ns smart-mirror.coordinates-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [smart-mirror.coordinates :as coordinates]))

(defspec ->coordinates-spec-comforms 50
  (prop/for-all [str-coordinates (s/gen ::coordinates/str-coordinates)]
                (is (s/valid? ::coordinates/coordinates
                              (coordinates/->coordinates str-coordinates)))))
