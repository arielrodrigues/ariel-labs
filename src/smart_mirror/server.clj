(ns smart-mirror.server
  (:require [smart-mirror.system :as system]))

(defn -main!
  []
  (println "Starting the server...")
  (system/create-and-start-system!))
