(ns common.http-client
  (:require [com.stuartsierra.component :as component]))

(defrecord HttpClient []
  component/Lifecycle
  
  (start [component])
  (stop [component]))

(defn new-http-client []
  (map->HttpClient {}))

(defrecord MockHttpClient [mock-http-server]
  component/Lifecycle

  (start [component]
    (assoc component :service (get mock-http-server :service)))

  (stop [component]
    (dissoc component :service)))

(defn new-mock-http-client []
  (map->MockHttpClient {}))

