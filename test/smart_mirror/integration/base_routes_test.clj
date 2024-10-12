(ns smart-mirror.integration.base-routes-test
  (:require [common.test :as common-test]
            [matcher-combinators.matchers :as m]
            [smart-mirror.integration.setup :refer [defflow]]
            [state-flow.api :as flow :refer [flow]]
            [state-flow.assertions.matcher-combinators :refer [match?]]))

(defflow health-test
  (flow "Given the health endpoint is called"
        (flow "When nothing is going bad"
              [{:keys [status body]} (common-test/request :get "/api/health"
                                                          :headers {"Accept" "application/json"})]
              (flow "Then a successful HTTP response with the components is expected"
                    (match? 200 status)
                    (match?
                     {:http-server "I'm doing fine."}
                     body)))))

(defflow version-test
  (flow "Given the version endpoint is called"
        [{:keys [status body]} (common-test/request :get "/api/version"
                                                    :headers {"Accept" "application/json"})]
        (flow "Then a successful HTTP with the version is expected"
              (match? 200 status)
              (match?
               {:version (m/pred string?)}
               body))))

(defflow metrics-test
  (flow "Given the metrics endpoint is called"
        [{:keys [status body]} (common-test/request :get "/api/metrics"
                                                    :headers {"Accept" "application/json"})]
        (flow "Then a successful HTTP with the metrics is expected"
              (match? 200 status)
              (match?
               (m/pred vector?)
               body))))
