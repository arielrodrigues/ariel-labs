(ns common.routes
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string]
            [common.interceptors :as common.interceptors]
            [io.pedestal.http.route :as io.route]))

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
  [path [method {:keys [handler]}]]
  (check-route-contraints! path method handler)
  (let [prefixed-path (include-api-prefix-on-path path)
        method+path (-> method
                        name
                        clojure.string/upper-case
                        (str prefixed-path)
                        keyword)]
    [prefixed-path method handler :route-name method+path]))

(defn- expand-routes!*
  [[path http-methods]]
  (map (partial path+method->pedestal-route! path) http-methods))

(s/def ::route-name keyword?)
(s/def ::method ::valid-http-methods)
(s/def ::interceptors ::common.interceptors/interceptors)
(s/def ::path-params  (s/* (s/cat :key keyword? :val string?)))

(s/def ::expanded-routes
  (s/coll-of
   (s/keys :req-un [::path
                    ::route-name
                    ::method
                    ::interceptors
                    ::path-params])))

(s/fdef expand-routes!
  :args (s/cat :routes ::routes)
  :ret ::expanded-routes)

(defn expand-routes!
  [routes]
  (->> routes
       (partition 2)
       (mapcat expand-routes!*)
       set
       io.route/expand-routes))
