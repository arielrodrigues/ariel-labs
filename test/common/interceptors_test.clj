(ns common.interceptors-test
  (:require [clojure.set]
            [clojure.spec.alpha :as s]
            [clojure.test :refer [testing is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [common.interceptors]
            [common.routes :as common-routes]))

#dbg
 (defn- every-handler?*
   [pred path-map]
   (let [res (update-vals path-map (fn [{:keys [handler]}] (pred handler)))]
     (every? (comp true? boolean) (vals res))))

#dbg
(defn- every-handler?
  [pred routes]
  (every? true?
          (map (fn [[_path path-map]]
                 (every-handler?* pred path-map))
               (partition 2 routes))))

(defspec routes->routes+common-interceptors___spec-is-respected 50
  (prop/for-all [routes (s/gen ::common-routes/routes)]
                (let [result (common.interceptors/routes->routes+common-interceptors routes)]
                  (testing "Given a well-formed routes collection.
                          When the include common interceptors fn is called.
                          Then it returns a well-formed routes collection"
                    (is (s/valid? ::common-routes/routes result))))))

(defspec routes->routes+common-interceptors____common-interceptors-are-correctly-injected 50
  (prop/for-all [routes (s/gen ::common-routes/routes)]
                (let [result (common.interceptors/routes->routes+common-interceptors routes)]
                  (testing "Given a well-formed routes collection.
                            When the include common interceptors fn is called.
                            Then the common interceptors were injected into the request handler."
                    (is (or (empty? result)
                            (every-handler? (fn [handler]
                                              (clojure.set/subset?
                                               (set common.interceptors/common-interceptors)
                                               (set handler))) result)))))))

 (defspec routes->routes+common-interceptors____request-handler-is-present 50
   (prop/for-all [routes (s/gen ::common-routes/routes)]
                 (let [result (common.interceptors/routes->routes+common-interceptors routes)]
                   (testing "Given a well-formed routes collection.
                                         When the include common interceptors fn is called.
                                         Then the provided request handler is still present."
                     (is (or (empty? result)
                             (every-handler? (fn [handler]
                                               (some #{common-routes/default-handler} handler))
                                             result)))))))
