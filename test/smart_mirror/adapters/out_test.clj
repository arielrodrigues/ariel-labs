(ns smart-mirror.adapters.out-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [smart-mirror.adapters.out :as out.adapter]
            [smart-mirror.specs.out :as out]
            [smart-mirror.weather :as weather]))

(defspec weather-forecast->wire--spec-conforms 50
  (prop/for-all [internal-model (s/gen ::weather/forecast)]
                (is (s/valid?
                     ::out/weather-forecast
                     (out.adapter/weather-forecast->wire internal-model)))))
