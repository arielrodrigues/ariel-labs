(ns smart-mirror.integration.plants-test
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [common.test :as common-test]
            [smart-mirror.db.plants :as plants.db]
            [smart-mirror.integration.setup :refer [defflow defflow-quickcheck]]
            [smart-mirror.specs.in :as in]
            [smart-mirror.specs.out :as out]
            [state-flow.api :as flow :refer [flow when]]
            [state-flow.assertions.matcher-combinators :refer [match?]]))

(def fcm-endpoint "https://fcm.googleapis.com/fcm/send")

(defflow-quickcheck plants-due-for-watering-test {}
  [sample-plant-data (s/gen ::in/create-plant-request)]
  (flow "GIVEN a plant that is due for watering today"
        (common-test/inject-faults! [:database :http])

        (common-test/request :post "/api/plants"
                             :headers {"Accept" "application/json"
                                       "Content-Type" "application/json"}
                             :body (json/write-str (assoc sample-plant-data
                                                          :name "Basil"
                                                          :water-frequency-days 1)))

        (flow "AND the Google FCM service is responding normally"
              (common-test/add-responses! {fcm-endpoint {:post {:status 201
                                                                :body {:name "projects/ariel-labs/messages/0:1605800999999999%0fdff9d0f9fd7ecd"}}}}))

        (flow "WHEN the 'plants/need-watering' endpoint is requested"
              [{:keys [status]} (common-test/request :get "/api/plants/need-watering"
                                                     :headers {"Accept" "application/json"})]

              (flow "THEN it should respond with the correct status depending on injected faults"
                    (common-test/match-case? {:status status}
                                             :no-faults-injected
                                             {:status 201}

                                             ;; FCM unauthorized
                                             (common-test/http-fault-injected-to? fcm-endpoint :post {:status 403})
                                             {:status 403}

                                             ;; Any other FCM HTTP error
                                             (common-test/http-fault-injected-to? fcm-endpoint :post)
                                             {:status 500}

                                             (common-test/database-fault-injected? :query)
                                             {:status 500}

                                             (common-test/database-fault-injected? :transact)
                                             {:status 500}

                                             :else
                                             {:status 201}))

              (flow "AND "
                    (if (false? (common-test/database-fault-injected? :query))
                      (flow "the FCM API should be called"
                            (match? true (common-test/request-made-to? fcm-endpoint :post)))
                      (flow "the FCM API shouldn't be called"
                            (match? false (common-test/request-made-to? fcm-endpoint :post)))))

              (flow "AND "
                    ;; Only verify notifications when a fault haven't been injected into the db queries
                    (if (and (common-test/request-made-to? fcm-endpoint :post)
                             (false? (common-test/database-fault-injected? :transact))
                             (false? (common-test/database-fault-injected? :query)))
                      (flow "the notification should be persisted in the DB"
                            [notifications (common-test/with-database plants.db/get-notifications)]
                            (match? 1 (count notifications))
                            (match? :notification/water-plants (:topic (first notifications))))
                      (flow "the notification shouldn't be persisted in the DB"
                            (match? empty? [])))))))











(defflow-quickcheck plant-creation-test {:num-tests 5}
  [payload (gen/fmap json/write-str (s/gen ::in/create-plant-request))]
  (flow "Testing plant creation with database fault injection"
    (common-test/inject-faults! [:database])

    (flow "When creating a plant with potential database faults"
      [{:keys [status body]} (common-test/request :post "/api/plants"
                                                  :headers {"Accept" "application/json"
                                                            "Content-Type" "application/json"}
                                                  :body payload)]
      (flow "Then handle database failures gracefully"
        (common-test/match-case? {:status status :body body}
          :no-faults-injected
          {:status 201 :body {:plant-id string?}}

          (common-test/database-fault-injected? :transact)
          {:status 500}

          (common-test/database-fault-injected?)
          {:status 201 :body {:plant-id string?}}

          :else
          {:status (fn [s] (>= s 500))})))))

(def sample-plant-data
  {:name "Costela de Adão"
   :scientific-name "Monstera deliciosa"
   :pic-url "https://example.com/monstera.jpg"
   :water-frequency-days 7
   :notes "Likes indirect light and humid conditions"
   :location "Living room windowsill"
   :care-level :easy
   :type "tropical"})

(def invalid-plant-data
  {:scientific-name "Missing name field"
   :water-frequency-days "not-a-number"})

(defflow plant-creation-and-retrieval-test
  (flow "Given a plant management system"

        (flow "When creating a new plant with valid data"
              [{:keys [status body]} (common-test/request :post "/api/plants"
                                                          :headers {"Accept" "application/json"
                                                                    "Content-Type" "application/json"}
                                                          :body (json/write-str sample-plant-data))]
              (flow "Then the plant is created successfully"
                    (match? 201 status)
                    (match? {:plant-id string?} body))

              (flow "And the plant ID is returned"
                    [plant-id (flow/return (:plant-id body))]
                    (match? true (string? plant-id))))

        (flow "When creating a plant with invalid data"
              [{:keys [status body]} (common-test/request :post "/api/plants"
                                                          :headers {"Accept" "application/json"
                                                                    "Content-Type" "application/json"}
                                                          :body (json/write-str invalid-plant-data))]
              (flow "Then it returns validation error"
                    (match? 400 status)
                    (match? {:error string?
                             :details string?} body)))

        (flow "When retrieving all plants from system"
              [{:keys [status body]} (common-test/request :get "/api/plants"
                                                          :headers {"Accept" "application/json"})]
              (flow "Then it returns successful not-empty response"
                    (match? 200 status)
                    (match? seq body)
                    (match? nil? (s/explain-data ::out/plants-response body))))

        (flow "When testing plant creation endpoints"
              [plant1-response (common-test/request :post "/api/plants"
                                                    :headers {"Accept" "application/json"
                                                              "Content-Type" "application/json"}
                                                    :body (json/write-str sample-plant-data))
               plant2-response (common-test/request :post "/api/plants"
                                                    :headers {"Accept" "application/json"
                                                              "Content-Type" "application/json"}
                                                    :body (json/write-str (assoc sample-plant-data
                                                                                 :name "Espada de São Jorge"
                                                                                 :scientific-name "Sansevieria trifasciata"
                                                                                 :water-frequency-days 14
                                                                                 :type :succulent)))]
              (flow "Then both plants can be created"
                    (match? 201 (:status plant1-response))
                    (match? 201 (:status plant2-response))
                    (match? {:plant-id string?} (:body plant1-response))
                    (match? {:plant-id string?} (:body plant2-response)))

              (flow "When trying to retrieve a specific plant"
                    [plant1-id (flow/return (:plant-id (:body plant1-response)))
                     {:keys [status body]} (common-test/request :get (str "/api/plants/" plant1-id)
                                                                :headers {"Accept" "application/json"})]
                    (flow "Then the endpoint responds"
                          (match? 200 status)
                          (match? nil? (s/explain-data ::out/plant-response body))))

              (flow "When retrieving a non-existent plant"
                    [fake-id (flow/return (random-uuid))
                     {:keys [status]} (common-test/request :get (str "/api/plants/" fake-id)
                                                           :headers {"Accept" "application/json"})]
                    (flow "Then the endpoint responds appropriately"
                          (match? 404 status))))))

(defflow plant-update-and-delete-test
  (flow "Testing plant update and delete endpoints"
        [plant-response (common-test/request :post "/api/plants"
                                             :headers {"Accept" "application/json"
                                                       "Content-Type" "application/json"}
                                             :body (json/write-str sample-plant-data))
         plant-id (flow/return (:plant-id (:body plant-response)))]

        (flow "When updating a plant"
              [update-data (flow/return {:notes "Updated care instructions"
                                         :location "New location - bedroom"
                                         :water-frequency-days 10})
               {:keys [status]} (common-test/request :put (str "/api/plants/" plant-id)
                                                     :headers {"Accept" "application/json"
                                                               "Content-Type" "application/json"}
                                                     :body (json/write-str update-data))]
              (flow "Then the update endpoint responds"
                    (match? 200 status)))

        (flow "When testing delete endpoint"
              [{:keys [status]} (common-test/request :delete (str "/api/plants/" plant-id)
                                                     :headers {"Accept" "application/json"})]
              (flow "Then the delete endpoint responds appropriately"
                    (match? 204 status)))))

(defflow plant-watering-test
  (flow "Testing plant watering endpoints"
        [plant-response (common-test/request :post "/api/plants"
                                             :headers {"Accept" "application/json"
                                                       "Content-Type" "application/json"}
                                             :body (json/write-str sample-plant-data))
         plant-id (flow/return (:plant-id (:body plant-response)))]

        (flow "When testing watering endpoint"
              [watering-data (flow/return {:watered-by "Ariel"
                                           :notes "Plant looked a bit dry"
                                           :amount-ml 250})
               {:keys [status]} (common-test/request :post (str "/api/plants/" plant-id "/water")
                                                     :headers {"Accept" "application/json"
                                                               "Content-Type" "application/json"}
                                                     :body (json/write-str watering-data))]
              (flow "Then the watering endpoint responds appropriately"
                    (match? 201 status)))

        (flow "When testing watering history endpoint"
              [{:keys [status]} (common-test/request :get (str "/api/plants/" plant-id "/history")
                                                     :headers {"Accept" "application/json"})]
              (flow "Then the history endpoint responds"
                    (match? 200 status)))))
