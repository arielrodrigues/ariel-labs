(ns smart-mirror.system
  (:require [common.http-server :refer [new-http-server new-mock-http-server]]
            [common.http-client :refer [new-http-client new-mock-http-client]]
            [common.system :as system]
            [com.stuartsierra.component :as component]
            [smart-mirror.http-in :as http-in]))

(def base-system-map
  {:http-server (new-http-server http-in/route-map)
   :http-client (new-http-client)})

(def test-system-map
  (merge
   base-system-map
        {:http-server (new-mock-http-server http-in/route-map)
         :http-client (component/using (new-mock-http-client)
                                       {:mock-http-server :http-server})}))

(defn create-and-start-system!
  ([] (system/bootstrap! base-system-map))
  ([system-map] (system/bootstrap! system-map)))

(defn stop-system!
  []
  (system/stop-system!))

(defn restart-system!
  []
  (stop-system!)
  (create-and-start-system!))
