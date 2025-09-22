(ns smart-mirror.integration.ariel-test
  (:require [clojure.spec.alpha :as s]
            [matcher-combinators.matchers :as m]
            [smart-mirror.integration.setup :refer [defflow-quickcheck]]
            [state-flow.api :refer [flow match?]]))

(defflow-quickcheck ariel-test {}
  [x (s/gen pos-int?)
   y (s/gen pos-int?)]
  (flow "x + y should be bigger than x and than y (for pos int)"
        (match? (m/pred #(> % x) (str (+ x y) " should be > " x)) (+ x y))
        (match? (m/pred #(> % y) (str (+ x y) " should be > " y)) (+ x y))))
