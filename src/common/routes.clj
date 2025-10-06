(ns common.routes
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string]
            [com.stuartsierra.component :as component]
            [common.interceptors]
            [common.system]
            [io.pedestal.http.route :as io.route]
            [io.pedestal.http.route.definition.table :as table]))

(def valid-http-methods
  #{:get :post :put :patch :delete :options :head})
(s/def ::valid-http-methods valid-http-methods)

(def default-handler identity)

(s/def ::handler (s/spec ifn?
                         :gen #(gen/return default-handler)))

(s/def ::method-map
  (s/keys :req-un [::handler]))

(s/def ::path-map
  (s/map-of ::valid-http-methods ::method-map))

(s/def ::path
  (s/with-gen (s/and string?
                     (complement empty?)
                     #(clojure.string/starts-with? % "/"))
    (fn [] (gen/fmap (partial str "/") (gen/string-alphanumeric)))))

(s/def ::route-def
  (s/cat :path ::path :path-map ::path-map))

(s/def ::routes (s/* ::route-def))

(s/fdef valid-http-method?
  :args keyword?
  :ret boolean?)

(defn- valid-http-method?
  [method]
  (contains? valid-http-methods method))

(def invalid-http-method? (complement valid-http-method?))

(defn- invalid-handler?
  [handler]
  (nil? handler))

(defn- include-api-prefix-on-path
  [path]
  (str "/api" path))

(defn- check-route-contraints!
  [path method handler]
  (cond
    (invalid-http-method? method)
    (throw (Exception. (str "Invalid HTTP method" method)))

    (invalid-handler? handler)
    (throw (Exception. (str "A handler is expected: " method " " path)))))

(defn- path+method->pedestal-route!
  [path [method {:keys [handler name constraints]}]]
  (check-route-contraints! path method handler)
  (let [prefixed-path (include-api-prefix-on-path path)
        route-name (or name
                       (-> method
                           clojure.core/name
                           clojure.string/upper-case
                           (str prefixed-path)
                           keyword))]
    (if (some? constraints)
      [prefixed-path method handler :route-name route-name :constraints constraints]
      [prefixed-path method handler :route-name route-name])))

(defn- expand-routes!*
  [[path http-methods]]
  (map (partial path+method->pedestal-route! path) http-methods))

(s/def ::route-name keyword?)
(s/def ::method ::valid-http-methods)
(s/def ::path-params  (s/* (s/cat :key keyword? :val string?)))

(s/def ::expanded-routes
  (s/coll-of
   (s/keys :req-un [::path
                    ::route-name
                    ::method
                    :common.interceptors/interceptors
                    ::path-params])))

(defn- custom-dsl->terse-routes
  "Convert custom DSL format to terse syntax format.
   Transforms ['/path' {:get {:handler fn :name :route-name}}]
   to terse format ['/path' {:get fn}] while preserving interceptors and metadata."
  [routes common-interceptors]
  (let [add-api-prefix (fn [path] (str "/api" path))
        convert-method-map (fn [path method-map]
                            (reduce-kv
                             (fn [acc method {:keys [handler name constraints]}]
                               (let [final-handler (if common-interceptors
                                                    (conj common-interceptors handler)
                                                    handler)
                                     route-name (or name
                                                    (-> method
                                                        clojure.core/name
                                                        clojure.string/upper-case
                                                        (str (add-api-prefix path))
                                                        keyword))]
                                 (assoc acc method
                                        (cond-> final-handler
                                          constraints (with-meta {:route-name route-name :constraints constraints})
                                          (not constraints) (with-meta {:route-name route-name})))))
                             {}
                             method-map))]
    (->> routes
         (partition 2)
         (mapv (fn [[path method-map]]
                 [(add-api-prefix path) (convert-method-map path method-map)])))))

(s/fdef expand-routes!
  :args (s/cat :routes ::routes)
  :ret ::expanded-routes)
(defn expand-routes!
  [routes]
  ;; Use vector instead of set to preserve route ordering
  (->> routes
       (partition 2)
       (mapcat expand-routes!*)
       vec  ; Use vector instead of set to preserve ordering
       table/table-routes))

(defn- inject-common-interceptors
  [path-map common-interceptors]
  (update-vals path-map (fn [v]
                          (let [handler (get v :handler)]
                            (assoc v :handler (conj common-interceptors handler))))))

(defn ->routes+common-interceptors
  [routes common-interceptors]
  (->> routes
       (partition 2)
       (map (fn [[path path-map]]
              [path (inject-common-interceptors path-map common-interceptors)]))
       flatten))

;; ---- component ----

(defrecord Routes [routes components-name validation-specs]
  component/Lifecycle
  (start [component]
    (let [get-components (fn [] (select-keys @common.system/system components-name))
          interceptors (if validation-specs
                         (common.interceptors/common-interceptors-with-validation get-components validation-specs)
                         (common.interceptors/common-interceptors get-components))]
      (assoc component :routes (-> routes
                                   (->routes+common-interceptors interceptors)
                                   expand-routes!))))
  (stop [component]
    (dissoc component :routes)))

(defn new-routes
  ([routes components-name]
   (map->Routes {:routes routes :components-name components-name}))
  ([routes components-name validation-specs]
   (map->Routes {:routes routes
                 :components-name components-name
                 :validation-specs validation-specs})))
