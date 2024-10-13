(ns common.http-client
  (:require [com.stuartsierra.component :as component]
            [common.protocols :as protocols]
            [org.httpkit.client :as http]))

(def default-headers
  {"Content-Type"    "application/json; charset=utf-8"})

(def default-options {:timeout 30000    ; ms
                      :keepalive 60000
                      :insecure? false
                      :headers default-headers
                      :user-agent "ariel-labs/http-client"})

;;; ---- components ----

(defrecord HttpClient [default-options]
  protocols/HttpClient
  (req! [req-map]
    (let [req-map* (merge default-options req-map)]
      (http/request req-map*)))

  component/Lifecycle
  (start [component] component)
  (stop [component] component))

(defn new-http-client [options]
  (map->HttpClient (merge default-options options)))

;; --

(def *responses* (atom {}))

(defn with-responses [responses f]
  (let [before @*responses*]
    (reset! *responses* responses)
    (f)
    (reset! *responses* before)))

(defn add-responses! [responses]
  (swap! *responses* merge responses))

(defrecord MockHttpClient [mock-http-server]
  protocols/HttpClient
  (req! [{:keys [method url]}]
    (get-in @*responses* [url method]))

  component/Lifecycle
  (start [component]
    (assoc component :service (get mock-http-server :service)))
  (stop [component]
    (dissoc component :service)))

(defn new-mock-http-client []
  (map->MockHttpClient {}))

