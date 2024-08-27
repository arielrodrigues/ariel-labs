(ns smart-mirror.system
  (:require [io.pedestal.http :as http]
            [smart-mirror.http-in :as http-in]
            [common.http-server :as http-server]
            [common.system :as system]))

(def base-system-map
  {:http-server (http-server/new-http-server http-in/route-map)})

(defn create-and-start-system!
  []
  (system/bootstrap! base-system-map))

(defn stop-system!
  []
  (system/stop-system!))

(defn restart-system!
  []
  (system/stop-system!)
  (system/start-system!))
