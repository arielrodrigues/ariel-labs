(ns common.interceptors
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.content-negotiation :as content-negotiation]
            [io.pedestal.http.route :as route]
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

(def uuid-path-params-interceptor
  {:name ::uuid-path-params
   :enter (fn [context]
            (let [path-params (get-in context [:request :path-params] {})
                  parsed-params (reduce-kv
                                 (fn [acc k v]
                                   (if (= k :id)
                                     (if-let [uuid (parse-uuid v)]
                                       (assoc acc k uuid)
                                       (reduced {:error "Invalid ID"}))
                                     (assoc acc k v)))
                                 {}
                                 path-params)]
              (if (:error parsed-params)
                (assoc context :response {:status 400 :body {:error "Invalid ID"}})
                (assoc-in context [:request :path-params] parsed-params))))})

(def service-error-handler
  (error-dispatch [context ex]

                  [{:exception-type :bad-request}]
                  (assoc context :response {:status 400 :body (-> ex Throwable->map :cause)})

                  [{:exception-type :unauthorized}]
                  (assoc context :response {:status 401 :body (-> ex Throwable->map :cause)})

                  [{:exception-type :forbidden}]
                  (assoc context :response {:status 403 :body (-> ex Throwable->map :cause)})

                  [{:exception-type :not-found}]
                  (assoc context :response {:status 404 :body (-> ex Throwable->map :cause)})

                  [{:exception-type :timeout}]
                  (assoc context :response {:status 408 :body (-> ex Throwable->map :cause)})

                  [{:exception-type :internal-server-error}]
                  (assoc context :response {:status 500 :body (-> ex Throwable->map :cause)})

                  [{:exception-type :bad-gateway}]
                  (assoc context :response {:status 502 :body (-> ex Throwable->map :cause)})

                  :else
                  (do (print ex)
                      (assoc context :response {:status 500 :body {:message "Internal server error."}}))))

(defn pred-to-message
  "Convert spec predicates to human-readable messages"
  [pred]
  (condp = pred
    `string? "must be a string"
    `pos-int? "must be a positive integer"
    `uuid? "must be a valid UUID"
    `inst? "must be a valid date/time"
    `(s/nilable string?) "must be a string or null"
    `(s/nilable pos-int?) "must be a positive integer or null"
    ;; Add more predicate mappings as needed
    (str "must satisfy: " pred)))

(defn spec-error->human-readable
  "Transform spec explain-data into human-readable error messages"
  [explain-data]
  (->> (:clojure.spec.alpha/problems explain-data)
       (map (fn [{:keys [path pred val]}]
              (let [field-path (if (empty? path) "root" (str/join "." (map name path)))]
                (format "Field '%s': %s (got: %s)"
                        field-path
                        (pred-to-message pred)
                        (pr-str val)))))
       (str/join ", ")))

(defn request-validation-interceptor
  "Interceptor that validates request payloads against clojure.spec schemas"
  [route-specs]
  {:name ::request-validation
   :enter (fn [context]
            (let [route-name (get-in context [:route :route-name])
                  json-params (get-in context [:request :json-params])
                  accepted-content-type (get-in context [:request :accept :field] "application/json")
                  spec (get route-specs route-name)]
              (if (and spec json-params)
                (if (s/valid? spec json-params)
                  context
                  (let [explain-data (s/explain-data spec json-params)
                        error-message (spec-error->human-readable explain-data)]
                    (assoc context :response {:status 400
                                              :body  (coerce-body {:error "Validation failed"
                                                                   :details error-message}
                                                                  accepted-content-type)})))
                context)))})

(defn common-interceptors-with-validation
  [get-components validation-specs]
  [(inject-components get-components)
   (body-params/body-params)
   route/path-params-decoder
   uuid-path-params-interceptor
   (request-validation-interceptor validation-specs)
   service-error-handler
   content-negotiation-interceptor
   coerce-body-interceptor])

(defn common-interceptors
  [get-components]
  [(inject-components get-components)
   (body-params/body-params)
   route/path-params-decoder
   uuid-path-params-interceptor
   service-error-handler
   content-negotiation-interceptor
   coerce-body-interceptor])
