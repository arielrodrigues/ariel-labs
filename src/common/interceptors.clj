(ns common.interceptors
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [io.pedestal.http.content-negotiation :as content-negotiation]
            [io.pedestal.interceptor.error :refer [error-dispatch]]))

(s/def ::name (s/nilable keyword?))
(s/def ::enter (s/nilable ifn?))
(s/def ::leave (s/nilable ifn?))
(s/def ::error (s/nilable ifn?))

(s/def ::interceptor (s/keys :req-un [::name
                                      ::enter
                                      ::leave
                                      ::error]))

(s/def ::interceptors (s/coll-of ::interceptor :gen-max 5))

(def supported-content-types
  ["text/plain"
   "application/edn"
   "application/json"])

(defn- coerce-body [body content-type]
  (case content-type
    "text/plain" body
    "application/edn" (pr-str body)
    "application/json" (json/write-str body)
    body))

(def content-negotiation-interceptor
  (content-negotiation/negotiate-content supported-content-types))

(def coerce-body-interceptor
  {:name ::coerce-body
   :leave (fn [context]
            (let [default-content-type "application/json"
                  accepted-content-type (get-in context [:request :accept :field] default-content-type)
                  response (get context :response)
                  body (-> context :response :body)]
              (assoc context :response (assoc response
                                              :headers {"Content-Type" accepted-content-type}
                                              :body (coerce-body body accepted-content-type)))))})

(defn inject-components [get-components]
  {:name ::inject-components
   :enter (fn [context]
            (assoc-in context [:request :components] (get-components)))})

(def service-error-handler
  (error-dispatch [context ex]

                  [{:exception-type :bad-request}]
                  (assoc context :response {:status 400 :body (-> ex Throwable->map :cause)})

                  [{:exception-type :not-found}]
                  (assoc context :response {:status 404 :body (-> ex Throwable->map :cause)})

                  :else
                  (do
                    ;; (print ex)
                    (assoc context :response {:status 500 :body {:message "Internal server error."}}))))


(defn common-interceptors
  [get-components]
  [(inject-components get-components)
   service-error-handler
   content-negotiation-interceptor
   coerce-body-interceptor])
