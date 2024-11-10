(ns smart-mirror.user
  (:require [flow-storm.api :as fs-api]
            [smart-mirror.system :as system]))

(comment
  (fs-api/local-connect)

  (require '[clojure.tools.namespace.repl :as repl])
  (repl/refresh)

  (system/create-and-start-system!)

  (system/stop-system!))
