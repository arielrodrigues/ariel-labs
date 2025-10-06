(ns common.interceptors-test
  (:require [clojure.test :refer [deftest is testing]]
            [common.interceptors :as interceptors]))

(deftest uuid-path-params-interceptor-test
  (testing "valid UUID is parsed and replaced"
    (let [uuid-str "550e8400-e29b-41d4-a716-446655440000"
          context {:request {:path-params {:id uuid-str :other "value"}}}
          result ((:enter interceptors/uuid-path-params-interceptor) context)]
      (is (uuid? (get-in result [:request :path-params :id])))
      (is (= "value" (get-in result [:request :path-params :other])))))

  (testing "invalid UUID returns 400 error"
    (let [context {:request {:path-params {:id "invalid-uuid"}}}
          result ((:enter interceptors/uuid-path-params-interceptor) context)]
      (is (= 400 (get-in result [:response :status])))
      (is (= {:error "Invalid ID"} (get-in result [:response :body])))))

  (testing "non-ID path parameters are unchanged"
    (let [context {:request {:path-params {:name "test" :type "plant"}}}
          result ((:enter interceptors/uuid-path-params-interceptor) context)]
      (is (= {:name "test" :type "plant"} (get-in result [:request :path-params])))))

  (testing "empty path-params handled gracefully"
    (let [context {:request {:path-params {}}}
          result ((:enter interceptors/uuid-path-params-interceptor) context)]
      (is (= {} (get-in result [:request :path-params])))))

  (testing "missing path-params handled gracefully"
    (let [context {:request {}}
          result ((:enter interceptors/uuid-path-params-interceptor) context)]
      (is (= {} (get-in result [:request :path-params]))))))