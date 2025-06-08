(ns smart-mirror.system
  (:require [com.stuartsierra.component :as component]
            [common.config :refer [new-config]]
            [common.http-client :refer [new-http-client new-mock-http-client]]
            [common.http-server :refer [new-http-server new-mock-http-server]]
            [common.routes :refer [new-routes]]
            [common.system :as system]
            [smart-mirror.http-in :as http-in]))

(def injected-components
  "Components that will be injected into the http handlers"
  [:http-client :config])

(def base-system-map
  {:routes (new-routes http-in/route-map injected-components)
   :http-server (component/using (new-http-server 9090)
                                 {:routes :routes})
   :http-client (new-http-client {})
   :config (new-config)})

(def test-system-map
  (merge
   base-system-map
   {:http-server (component/using (new-mock-http-server)
                                  {:routes :routes})
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
