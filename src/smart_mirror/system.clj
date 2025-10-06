(ns smart-mirror.system
  (:require [com.stuartsierra.component :as component]
            [common.config :refer [new-config]]
            [common.datomic :refer [new-datomic-database new-mock-database]]
            [common.http-client :refer [new-http-client new-mock-http-client]]
            [common.http-server :refer [new-http-server new-mock-http-server]]
            [common.routes :refer [new-routes]]
            [common.system :as system]
            [common.gauth :as gauth]
            [smart-mirror.db.schema :as schema]
            [smart-mirror.http-in :as http-in]))

(def injected-components
  "Components that will be injected into the http handlers"
  [:http-client :config :google-auth-token-provider :database])

(def base-system-map
  {:routes (new-routes http-in/route-map injected-components http-in/route-validation-specs)
   :http-server (component/using (new-http-server 9090)
                                 {:routes :routes})
   :http-client (new-http-client {})
   :config (new-config)
   :google-auth-token-provider (gauth/new-google-auth-token-provider)
   :database (new-datomic-database)})

(def test-system-map
  (merge
   base-system-map
   {:http-server (component/using (new-mock-http-server)
                                  {:routes :routes})
    :http-client (component/using (new-mock-http-client)
                                  {:mock-http-server :http-server})
    :google-auth-token-provider (gauth/new-mock-token-provider "mock-test-token")
    :database (new-mock-database)}))

(defn install-schema! [system]
  (when-let [database (get system :database)]
    (when (:connection database) ; Only install schema for real databases
      (schema/install-schema (:connection database))))
  system)

(defn create-and-start-system!
  ([] (-> base-system-map
          system/bootstrap!
          install-schema!))
  ([system-map] (-> system-map
                    system/bootstrap!
                    install-schema!)))

(defn stop-system!
  []
  (system/stop-system!))

(defn restart-system!
  []
  (stop-system!)
  (create-and-start-system!))
