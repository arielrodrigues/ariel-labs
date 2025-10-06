(ns smart-mirror.adapters.out-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [smart-mirror.adapters.out :as out.adapter]
            [smart-mirror.calendar :as calendar]
            [smart-mirror.plants :as plants]
            [smart-mirror.specs.out :as out]
            [smart-mirror.time :as time]
            [smart-mirror.weather :as weather]))

(defspec weather-forecast->wire--spec-conforms 50
  (prop/for-all [internal-model (s/gen ::weather/forecast)]
                (is (s/valid?
                     ::out/weather-forecast
                     (out.adapter/weather-forecast->wire internal-model)))))

(defspec calendar->wire--spec-conforms 50
  (prop/for-all [internal-model (s/gen ::calendar/calendar)]
                (is (s/valid?
                     ::out/calendar
                     (out.adapter/calendar->wire internal-model)))))

(defspec times->wire--spec-conforms 50
  (prop/for-all [internal-model (s/gen (s/coll-of ::time/time :min-count 1 :gen-max 3))]
                (is (s/valid?
                     ::out/times
                     (out.adapter/times->wire internal-model)))))

;; Plant adapter tests
(defspec plant->wire--spec-conforms 50
  (prop/for-all [internal-model (s/gen ::plants/plant)]
                (is (s/valid?
                     ::out/plant-response
                     (out.adapter/plant->wire internal-model)))))

(defspec plants->wire--spec-conforms 50
  (prop/for-all [internal-models (s/gen (s/coll-of ::plants/plant :min-count 1 :gen-max 3))]
                (is (s/valid?
                     ::out/plants-response
                     (out.adapter/plants->wire internal-models)))))

(defspec watering->wire--spec-conforms 50
  (prop/for-all [internal-model (s/gen ::plants/watering)]
                (is (s/valid?
                     ::out/watering-response
                     (out.adapter/watering->wire internal-model)))))

(defspec waterings->wire--spec-conforms 50
  (prop/for-all [internal-models (s/gen (s/coll-of ::plants/watering :min-count 1 :gen-max 3))]
                (is (s/valid?
                     ::out/waterings-response
                     (out.adapter/waterings->wire internal-models)))))
