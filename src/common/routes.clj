(ns common.routes
  (:require [io.pedestal.http.route :as io.route]))

(def valid-http-methods
  #{:get :post :put :patch :delete :options :head :trace :connect})

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

(defn expand-route-map!*
  [[path http-methods]]
  (map (partial path+method->pedestal-route! path) http-methods))

(defn expand-route-map!
  [route-map]
  (->> route-map
       (partition 2)
       (mapcat expand-route-map!*)
       set
       io.route/expand-routes))
