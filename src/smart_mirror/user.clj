(ns smart-mirror.user
  (:require [flow-storm.api :as fs-api]))

(comment
  (fs-api/local-connect)
  #rtrace (reduce + (map inc (range 10))))
