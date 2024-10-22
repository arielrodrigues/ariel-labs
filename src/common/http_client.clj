(ns common.http-client
  (:require [clojure.data.json :as json]
            [com.stuartsierra.component :as component]
            [common.protocols.http-client :as protocols]
            [org.httpkit.client :as http]))

(def default-headers
  {"Content-Type"    "application/json; charset=utf-8"})

(def default-options {:timeout 30000    ; ms
                      :keepalive 60000
                      :insecure? false
                      :headers default-headers
                      :user-agent "ariel-labs/http-client"})

(defn in-response-error [url method response]
  (ex-info "HttpClient in-response error"
           {::url          url
            ::method       method
            ::cause        (:error response)}))

;;; ---- components ----

(defrecord HttpClient [default-options]
  protocols/HttpClient
  (req! [_this req-map]
    (let [req-map* (merge default-options req-map)
          response @(http/request req-map*)]
      (if (:error response)
        (throw (in-response-error (:url req-map*) (:method req-map*) response))
        (assoc response :body
               (json/read-str (:body response) :key-fn keyword)))))

  component/Lifecycle
  (start [component] component)
  (stop [component] component))

(defn new-http-client [options]
  (map->HttpClient (merge default-options options)))

;; --

(defn error? [{:keys [status]}]
  (<= 300 status))

(defrecord MockHttpClient [mock-http-server]
  protocols/HttpClient
  (req! [this {:keys [method url]}]
    (swap! (:*requests-log* this) assoc-in [url method] (inc (get-in (:*requests-log* this) [url method] 0)))

    (let [response (get-in
                    @(:*responses* this)
                    [url method]
                    {:status 404 :body {:message "not found"}})]
      (if (error? response)
        (throw (in-response-error url method {:error response}))
        response)))

  component/Lifecycle
  (start [component]
    (assoc component
           :service (get mock-http-server :service)
           :*responses* (atom {})
           :*requests-log* (atom {})))
  (stop [component]
    (dissoc component :service :*responses*)))


(defn new-mock-http-client []
  (map->MockHttpClient {}))
