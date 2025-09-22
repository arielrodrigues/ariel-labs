(ns smart-mirror.adapters.out-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [smart-mirror.adapters.out :as out.adapter]
            [smart-mirror.calendar :as calendar]
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
