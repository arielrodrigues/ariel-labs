(ns smart-mirror.time-test
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string]
            [clojure.test :refer [is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [common.time :as common-time]
            [smart-mirror.time :as time]))

(defspec ->time-spec-conforms 50
  (prop/for-all [zoned-date-time (s/gen ::common-time/zoned-date-time)]
                (is (s/valid? ::time/time (time/->time zoned-date-time)))))

(defn invalid-timezone-gen []
  (let [res (gen/generate (gen/string-alphanumeric))]
    (if (time/valid-timezone? res)
      (invalid-timezone-gen)
      (gen/return res))))

(defspec valid-timezone?-test 50
  (prop/for-all [valid-timezone (s/gen ::time/valid-timezone)
                 invalid-timezone (invalid-timezone-gen)]
                (do (is (time/valid-timezone? valid-timezone))
                    (is false? (time/valid-timezone? invalid-timezone)))))

(defspec time+zones-test 50
  (prop/for-all [zoned-date-time (s/gen ::common-time/zoned-date-time)
                 valid-timezones (s/gen (s/coll-of ::time/valid-timezone :gen-max 3))]
                (is (s/valid? (s/coll-of ::time/time)
                              (time/time+zones zoned-date-time valid-timezones)))))

(defspec qs->timezones 50
  (prop/for-all [valid-timezones (s/gen (s/coll-of ::time/valid-timezone :gen-max 5))]
                (let [qs (clojure.string/join " " valid-timezones)]
                  (is (time/valid-timezones? (time/qs->timezones qs))))))
