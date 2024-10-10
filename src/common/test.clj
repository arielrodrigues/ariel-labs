(ns common.test
  (:require [clojure.data.json :as json]
            [io.pedestal.test :refer [response-for]]
            [state-flow.api :as flow]))

(defn try-json-body
  [{:keys [status body] :as raw}]
  (try
    {:status status :body (json/read-json body)}
    (catch Exception e
      raw)))

(defn request [method url & options]
  (flow/flow "do a request"
             [service (flow/get-state (comp :service :http-client :system))]
             (if (some? service)
               (-> (apply response-for service method url options)
                   try-json-body
                   flow/return)
               (throw (ex-info "Test system isn't initiated!" {})))))
