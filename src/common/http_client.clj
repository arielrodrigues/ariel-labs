(ns common.http-client
  (:require [com.stuartsierra.component :as component]
            [common.protocols.http-client :as protocols]
            [org.httpkit.client :as http]
            [state-flow.api :as flow]))

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
  (req! [_this req-map]
    (let [req-map* (merge default-options req-map)]
      (http/request req-map*)))

  component/Lifecycle
  (start [component] component)
  (stop [component] component))

(defn new-http-client [options]
  (map->HttpClient (merge default-options options)))

;; --
(defrecord MockHttpClient [mock-http-server]
   protocols/HttpClient
   (req! [this {:keys [method url]}]
     (get-in
      @(:*responses* this)
      [url method]
      {:status 404 :body {:message "not found"}}))

   component/Lifecycle
   (start [component]
     (assoc component
            :service (get mock-http-server :service)
            :*responses* (atom {})))
   (stop [component]
     (dissoc component :service :*responses*)))


(defn new-mock-http-client []
  (map->MockHttpClient {}))

