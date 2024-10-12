(ns smart-mirror.integration.time-test
  (:require [common.test :as common-test]
            [common.time :as time]
            [smart-mirror.integration.setup :refer [defflow]]
            [state-flow.api :as flow :refer [flow]]
            [state-flow.assertions.matcher-combinators :refer [match?]]
            [state-flow.labs.state :as flow-labs]))

(def mocked-date-time
  (time/now "2024-10-10T20:49:10.386094+01:00[Europe/London]"))

(defflow get-time-test
  (flow "Given a request to get the time"
        (flow "When it doesn't include additional timezones"
              [{:keys [status body]} (flow-labs/with-redefs
                                      [time/now (constantly mocked-date-time)]
                                       (common-test/request :get "/api/time"
                                                            :headers {"Accept" "application/json"}))]
              (flow "Then the time is returned in the default timezone (Europe/London)"
                    (match? 200 status)
                    (match?
                     [{:utc-offset "+01:00"
                       :timezone {:name "Europe/London", :abbreviation "BST"}
                       :date-time "2024-10-10T20:49:10.386094+01:00[Europe/London]"
                       :timestamp 1728589750
                       :weekend? false}]
                     body)))

        (flow "When it includes one additional timezone"
              [{:keys [status body]} (flow-labs/with-redefs
                                      [time/now (constantly mocked-date-time)]
                                       (common-test/request :get "/api/time?include=America/Sao_Paulo"
                                                            :headers {"Accept" "application/json"}))]
              (flow "Then the time is returned in the default timezone (Europe/London) and the aditional one"
                    (match? 200 status)
                    (match?
                     [{:utc-offset "+01:00"
                       :timezone {:name "Europe/London", :abbreviation "BST"}
                       :date-time "2024-10-10T20:49:10.386094+01:00[Europe/London]"
                       :timestamp 1728589750
                       :weekend? false}
                      {:utc-offset "-03:00"
                       :timezone {:name "America/Sao_Paulo", :abbreviation "BRT"}
                       :date-time "2024-10-10T16:49:10.386094-03:00[America/Sao_Paulo]"
                       :timestamp 1728589750
                       :weekend? false}]
                     body)))

        (flow "When it includes more than one additional timezones"
              [{:keys [status body]} (flow-labs/with-redefs
                                      [time/now (constantly mocked-date-time)]
                                       (common-test/request :get "/api/time?include=America/Sao_Paulo+Europe/Berlin"
                                                            :headers {"Accept" "application/json"}))]
              (flow "Then the time is returned in the default timezone and the additional ones"
                    (match? 200 status)
                    (match?
                     [{:utc-offset "+01:00"
                       :timezone {:name "Europe/London", :abbreviation "BST"}
                       :date-time "2024-10-10T20:49:10.386094+01:00[Europe/London]"
                       :timestamp 1728589750
                       :weekend? false}
                      {:utc-offset "-03:00"
                       :timezone {:name "America/Sao_Paulo", :abbreviation "BRT"}
                       :date-time "2024-10-10T16:49:10.386094-03:00[America/Sao_Paulo]"
                       :timestamp 1728589750
                       :weekend? false}
                      {:utc-offset "+02:00",
                       :timezone {:name "Europe/Berlin", :abbreviation "CEST"},
                       :date-time "2024-10-10T21:49:10.386094+02:00[Europe/Berlin]",
                       :timestamp 1728589750,
                       :weekend? false}]
                     body)))

        (flow "When the included timezone is invalid"
              [{:keys [status]} (flow-labs/with-redefs
                                 [time/now (constantly mocked-date-time)]
                                  (common-test/request :get "/api/time?include=America/Sao_Paulo+America/Aracaju"
                                                       :headers {"Accept" "application/json"}))]
              (flow "Then it should return HTTP Bad request (400)"
                    (match? 400 status)))))
