(ns smart-mirror.time-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [common.time :as common-time]
            [smart-mirror.time :as time]))

(defspec ->time-spec-conforms 50
  (prop/for-all [zoned-date-time (s/gen ::common-time/zoned-date-time)]
                (is (s/valid? ::time/time (time/->time zoned-date-time)))))
