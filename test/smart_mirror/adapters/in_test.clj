(ns smart-mirror.adapters.in-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [smart-mirror.adapters.in :as in.adapter]
            [smart-mirror.plants :as plants]
            [smart-mirror.specs.in :as in]
            [smart-mirror.weather :as weather]))

(defspec wire->weather-forecast-spec-conforms 50
  (prop/for-all [wire-forecast (s/gen ::in/weather-forecast)]
                (is (s/valid?
                     ::weather/forecast
                     (in.adapter/wire->weather-forecast wire-forecast)))))

;; Plant adapter tests
(defspec wire->create-plant-spec-conforms 50
  (prop/for-all [request (s/gen ::in/create-plant-request)]
                (is (s/valid?
                     ::plants/plant-input
                     (in.adapter/wire->create-plant request)))))

(defspec wire->update-plant-spec-conforms 50
  (prop/for-all [request (s/gen ::in/update-plant-request)]
                (is (s/valid?
                     ::plants/plant-updates
                     (in.adapter/wire->update-plant request)))))

(defspec wire->water-plant-spec-conforms 50
  (prop/for-all [request (s/gen ::in/water-plant-request)]
                (is (s/valid?
                     ::plants/watering-input
                     (in.adapter/wire->water-plant request)))))
