(ns smart-mirror.adapters.in-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [smart-mirror.adapters.in :as in.adapter]
            [smart-mirror.specs.in :as in]
            [smart-mirror.weather :as weather]))

(defspec wire->weather-forecast-spec-conforms 50
  (prop/for-all [wire-forecast (s/gen ::in/weather-forecast)]
                (is (s/valid?
                     ::weather/forecast
                     (in.adapter/wire->weather-forecast wire-forecast)))))
