(ns smart-mirror.logic
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string]))
(defn- gen-coordinate
  []
  (gen/generate (gen/double* {:infinite? false :NaN? false})))

(s/def ::str-coordinates
  (s/with-gen
    (s/and string? (complement empty?))
    (fn [] (let [lat (gen-coordinate)
                 long (gen-coordinate)]
             (gen/return (str lat "," long))))))

(s/def ::coordinates
  (s/with-gen
    (s/cat :lat number? :long number?)
    (fn [] (let [lat (gen-coordinate)
                 long (gen-coordinate)]
             (gen/return [lat long])))))

(s/fdef ->coordinates
  :args (s/cat :loc ::str-coordinates)
  :ret ::coordinates)
(defn ->coordinates
  [loc]
  (->> #","
       (clojure.string/split loc)
       (map read-string)))
