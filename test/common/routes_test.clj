(ns common.routes-test
  (:require [clojure.set]
            [clojure.spec.alpha :as s]
            [clojure.test :refer [is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [common.interceptors]
            [common.routes :as common-routes]))

(def common-interceptors (common.interceptors/common-interceptors {}))

(defspec expand-routes!-test 50
  (prop/for-all [routes (s/gen ::common-routes/routes)]
                (let [routes-vec (vec routes)
                      route-def (if (seq routes-vec)
                                  (subvec routes-vec 0 2)
                                  [])
                      route-path (first route-def)
                      route-methods (second route-def)
                      expanded-route-def (common-routes/expand-routes! route-def)]

                  (testing "Given a well-formed routes collection. When expanded, Then the spec is respected"
                    (is (s/valid? ::common-routes/expanded-routes (common-routes/expand-routes! routes))))

                  (testing "Given an expanded-route, their :path matches the non-expanded form"
                    (is (every? #(= (str "/api" route-path) %) (map :path expanded-route-def))))

                  (testing "Given an expanded-route, all methods are equivalent to those in the non-expanded form"
                    (is (= (set (or (keys route-methods) [])) (set (map :method expanded-route-def))))))))


(defn- every-handler?*
   [pred path-map]
   (let [res (update-vals path-map (fn [{:keys [handler]}] (pred handler)))]
     (every? (comp true? boolean) (vals res))))

(defn- every-handler?
  [pred routes]
  (every? true?
          (map (fn [[_path path-map]]
                 (every-handler?* pred path-map))
               (partition 2 routes))))

(defspec ->routes+common-interceptors___spec-is-respected 50
  (prop/for-all [routes (s/gen ::common-routes/routes)]
                (let [result (common-routes/->routes+common-interceptors
                              routes
                              common-interceptors)]
                  (testing "Given a well-formed routes collection.
                          When the include common interceptors fn is called.
                          Then it returns a well-formed routes collection"
                    (is (s/valid? ::common-routes/routes result))))))

(defspec ->routes+common-interceptors____common-interceptors-are-correctly-injected 50
  (prop/for-all [routes (s/gen ::common-routes/routes)]
                (let [result (common-routes/->routes+common-interceptors
                              routes
                              common-interceptors)]
                  (testing "Given a well-formed routes collection.
                            When the include common interceptors fn is called.
                            Then the common interceptors were injected into the request handler."
                    (is (or (empty? result)
                            (every-handler? (fn [handler]
                                              (clojure.set/subset?
                                               (set common-interceptors)
                                               (set handler))) result)))))))

(defspec ->routes+common-interceptors____request-handler-is-present 50
  (prop/for-all [routes (s/gen ::common-routes/routes)]
                (let [result (common-routes/->routes+common-interceptors
                              routes
                              common-interceptors)]
                  (testing "Given a well-formed routes collection.
                                          When the include common interceptors fn is called.
                                          Then the provided request handler is still present."
                    (is (or (empty? result)
                            (every-handler? (fn [handler]
                                              (some #{common-routes/default-handler} handler))
                                            result)))))))
