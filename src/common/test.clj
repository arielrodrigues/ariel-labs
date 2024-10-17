(ns common.test
  (:require [clojure.data.json :as json]
            [io.pedestal.test :refer [response-for]]
            [state-flow.api :as flow]))

(defn- try-json-body
  [{:keys [status body] :as raw}]
  (try
    {:status status :body (json/read-json body)}
    (catch Exception e
      raw)))

(defn request [method url & options]
  (flow/flow "request>"
             [service (flow/get-state (comp :service :http-client :system))]
             (if (some? service)
               (-> (apply response-for service method url options)
                   try-json-body
                   flow/return)
               (throw (ex-info "Test system isn't initiated!" {})))))

(defn add-responses! [responses]
  (flow/flow "add-responses!>"
             [http-client (flow/get-state (comp :http-client :system))]
             (->> responses
                  (swap! (:*responses* http-client) merge)
                  flow/return)))

(defn remove-responses! [urls]
  (flow/flow "remove-responses!>"
             [http-client (flow/get-state (comp :http-client :system))]
             (->> urls
                  (apply swap! (:*responses* http-client) dissoc)
                  flow/return)))
