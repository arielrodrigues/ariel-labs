(ns smart-mirror.integration.weather-test
  (:require [common.test :as common-test]
            [smart-mirror.integration.setup :refer [defflow]]
            [state-flow.api :as flow :refer [flow]]
            [state-flow.assertions.matcher-combinators :refer [match?]]))

(def ipinfo-endpoint "https://ipinfo.io/json")
(def mock-ipinfo-response
  {:status 200
   :body {:city "Manchester",
          :region "England",
          :country "GB",
          :loc "53.4809,-2.2374",
          :timezone "Europe/London"}})

(def open-meteo-forecast-endpoint "https://api.open-meteo.com/v1/forecast")
(def mock-open-meteo-forecast-response
  {:status 200
   :body (:body mock-ipinfo-response)})

(reset! common-test/*injected-faults* {})

(defflow get-weather-forecast
  (flow "GIVEN that both IPInfo and Open Meteo forecast endpoints might be up and health (Faults might be injected)."
        (common-test/add-responses! {ipinfo-endpoint {:get mock-ipinfo-response}})
        (common-test/add-responses! {open-meteo-forecast-endpoint {:get mock-open-meteo-forecast-response}})
        (common-test/inject-faults!)

        (flow "WHEN a get weather forecast request is made."
              [response (common-test/request :get "/api/weather"
                                             :headers {"Accept" "application/json"})]

              (flow "THEN it must respond successfully if no faults were injected. Or return an error otherwise."
                    (common-test/match-case? response

                                             :no-faults-injected
                                             mock-ipinfo-response

                                             {ipinfo-endpoint {:get {:status (fn [status] (do (print status) (not= 200 status)))}}}
                                             {:status 300}

                                             {open-meteo-forecast-endpoint {:get {:status #(not= 200 %)}}}
                                             {:status 512}

                                             :else
                                             {:status 285})

                    (flow "AND the call to Open Meteo forecast should be skipped if the IPinfo request fails."
                          (flow/when (common-test/http-failure-injected-to? ipinfo-endpoint :get)
                            (match? false? (common-test/request-made-to? open-meteo-forecast-endpoint :get))))))))
