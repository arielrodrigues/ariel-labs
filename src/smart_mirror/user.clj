(ns smart-mirror.user
  (:require [flow-storm.api :as fs-api]))

(comment
  (fs-api/local-connect)

  (require '[clojure.tools.namespace.repl :as repl])
  (repl/refresh))
