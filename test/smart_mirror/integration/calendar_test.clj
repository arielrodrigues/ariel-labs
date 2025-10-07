(ns smart-mirror.integration.calendar-test
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [common.test :as common-test]
            [smart-mirror.adapters.in :as in.adapter]
            [smart-mirror.adapters.out :as out.adapter]
            [smart-mirror.integration.setup :refer [defflow-quickcheck]]
            [smart-mirror.specs.in :as in]
            [smart-mirror.specs.out :as out]
            [state-flow.api :as flow :refer [flow]]
            [state-flow.assertions.matcher-combinators :refer [match?]]))

(def google-calendar-endpoint "https://www.googleapis.com/calendar/v3/calendars/primary/events")

(defflow-quickcheck get-calendar-events-integration {}
  [mock-calendar-response (gen/fmap (fn [calendar] {:status 200 :body calendar})
                                    (s/gen ::in/calendar))]

  (flow "GIVEN that the Google Calendar API endpoint might be up and healthy (Faults might be injected)."
        (common-test/add-responses! {google-calendar-endpoint {:get mock-calendar-response}})
        (common-test/inject-faults! [:http :google-auth-token-provider])

        (flow "WHEN a get calendar events request is made."
              [response (common-test/request :get "/api/calendar"
                                             :headers {"accept" "application/json"})]

              (flow "THEN it must respond successfully if no faults were injected. Or return an error otherwise."
                    (common-test/match-case? response

                                             :no-faults-injected
                                             (fn [{:keys [body]}]
                                               (and (nil? (s/explain-data ::out/calendar body))
                                                    (match? body
                                                            (-> (:body mock-calendar-response)
                                                                in.adapter/wire->gcal
                                                                out.adapter/calendar->wire))))

                                             (common-test/fault-injected-to-google-auth-token-provider? :timeout)
                                             {:status 401}

                                             (common-test/fault-injected-to-google-auth-token-provider? :unauthorized)
                                             {:status 401}

                                             (common.test/http-fault-injected-to? google-calendar-endpoint :get {:status 403})
                                             {:status 403}

                                             :else
                                             {:status 502})))))
