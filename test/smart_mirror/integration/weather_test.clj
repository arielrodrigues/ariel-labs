(ns smart-mirror.integration.weather-test
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

(def ipinfo-endpoint "https://ipinfo.io/json")
(def mock-ipinfo-response
  {:status 200
   :body (gen/generate (s/gen ::in/ip-info))})

(def open-meteo-forecast-endpoint "https://api.open-meteo.com/v1/forecast")

(defflow-quickcheck get-weather-forecast
  [mock-open-meteo-forecast-response (gen/fmap (fn [forecast] {:status 200 :body forecast})
                                               (s/gen ::in/weather-forecast))]
  (flow "GIVEN that both IPInfo and Open Meteo forecast endpoints might be up and health (Faults might be injected)."
        (common-test/add-responses! {ipinfo-endpoint {:get mock-ipinfo-response}})
        (common-test/add-responses! {open-meteo-forecast-endpoint {:get mock-open-meteo-forecast-response}})
        (common-test/inject-faults!)

        (flow "WHEN a get weather forecast request is made."
              [response (common-test/request :get "/api/weather"
                                             :headers {"Accept" "application/json"})]

              (flow "THEN it must respond successfully if no faults were injected. Or return the expected error otherwise."
                    (common-test/match-case? response

                                             :no-faults-injected
                                             (fn [{:keys [body]}]
                                               (and (nil? (s/explain-data ::out/weather-forecast body))
                                                    (match? body
                                                            (-> (:body mock-open-meteo-forecast-response)
                                                                in.adapter/wire->weather-forecast
                                                                out.adapter/weather-forecast->wire))))

                                             (common-test/http-failure-injected-to? ipinfo-endpoint :get)
                                             {:status 502}

                                             (common-test/http-failure-injected-to? open-meteo-forecast-endpoint :get)
                                             {:status 502}

                                             :else
                                             {:status 500})

                    (flow "AND the call to Open Meteo forecast should be skipped if the IPinfo request fails."
                          (flow/when (common-test/http-failure-injected-to? ipinfo-endpoint :get)
                            (match? false? (common-test/request-made-to? open-meteo-forecast-endpoint :get))))))))
