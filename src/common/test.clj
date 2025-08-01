(ns common.test
  (:require [clojure.data.json :as json]
            [clojure.pprint]
            [clojure.spec.gen.alpha :as gen]
            [io.pedestal.test :refer [response-for]]
            [matcher-combinators.core]
            [medley.core :refer [map-kv deep-merge]]
            [state-flow.api :as flow]
            [state-flow.assertions.matcher-combinators :as state-flow.matchers]))

(defn- try-json-body
  [{:keys [status body] :as raw}]
  (try
    {:status status :body (json/read-json body)}
    (catch Exception e
      raw)))

(defn request [method url & options]
  (flow/flow "request>"
             [service (flow/get-state (comp :service :http-client :system))]
             (if (some? service)
               (-> (apply response-for service method url options)
                   try-json-body
                   flow/return)
               (throw (ex-info "Test system isn't initiated!" {})))))

(defn add-responses! [responses]
  (flow/flow "add-responses!>"
             [http-client (flow/get-state (comp :http-client :system))]
             (->> responses
                  (swap! (:*responses* http-client) merge)
                  flow/return)))

(defn remove-responses! [urls]
  (flow/flow "remove-responses!>"
    [http-client (flow/get-state (comp :http-client :system))]
    (->> urls
         (apply swap! (:*responses* http-client) dissoc)
         flow/return)))

;; fault injection
(def http-faults
  [{:status 401 :body {:message "unauthorized"}}
   {:status 403 :body {:message "forbidden"}}
   {:status 404 :body {:message "not-found"}}
   {:status 408 :body {:message "timeout"}}
   {:status 429 :body {:message "too many requests"}}
   {:status 408 :body {:message "timeout"}}
   {:status 500 :body {:message "internal server error"}}
   {:status 503 :body {:message "service unavailable"}}])

(def token-provider-faults
  [{:type :timeout}
   {:type :unauthorized}])


;; Seed coordination for reproducible tests
(defn get-test-seed []
  (or (when-let [seed-prop (System/getProperty "test.seed")]
        (Long/parseLong seed-prop))
      (System/currentTimeMillis)))

(def ^:dynamic *injected-faults* (atom {}))
(def default-fault-injection-likelihood 0.1)

;; Deterministic randomness for reproducible tests
(defn- deterministic-fault-choice [rng choices]
  (nth choices (.nextInt rng (count choices))))

(defn- deterministic-fault-probability [rng]
  (.nextDouble rng))

(defn- should-inject-fault? [rng likelihood]
  (< (deterministic-fault-probability rng) likelihood))

(defn- inject-fault-to-endpoint! [rng url methods+responses]
  (let [fault (deterministic-fault-choice rng http-faults)
        faulted-responses (into {} (map (fn [[method _]] [method fault]) methods+responses))]
    (swap! *injected-faults* deep-merge {url faulted-responses})
    [url faulted-responses]))

(defn- maybe-inject-fault-to-endpoint!
  ([rng url methods+responses]
   (maybe-inject-fault-to-endpoint! rng url methods+responses default-fault-injection-likelihood))
  ([rng url methods+responses likelihood]
   (if (should-inject-fault? rng likelihood)
     (inject-fault-to-endpoint! rng url methods+responses)
     [url methods+responses])))

(defn- maybe-inject-token-provider-fault!
  [rng]
  (when (should-inject-fault? rng default-fault-injection-likelihood)
    (let [fault (deterministic-fault-choice rng token-provider-faults)]
      (swap! *injected-faults* assoc :google-auth-token-provider fault)
      fault)))

(defn inject-faults!
  ([]
   (inject-faults! [:all]))
  ([fault-types]
   (reset! *injected-faults* {})
   (flow/flow "inject-faults!>"
              [test-seed (flow/get-state :test-seed)
               http-responses (flow/get-state (comp :*responses* :http-client :system))
               google-auth-token-provider-faults (flow/get-state (comp :*faults* :google-auth-token-provider :system))]
              (let [rng (java.util.Random. test-seed)
                    should-inject-http? (some #{:http :all} fault-types)
                    should-inject-token-provider? (some #{:google-auth-token-provider :all} fault-types)]
                (println "Fault injection using seed:" test-seed)
                (flow/flow "fault-injection>"
                           ;; Inject http faults conditionally
                           (if should-inject-http?
                             (-> http-responses
                                 (reset! (map-kv (fn [url methods+responses]
                                                   (maybe-inject-fault-to-endpoint! rng url methods+responses))
                                                 @http-responses))
                                 flow/return)
                             (flow/return nil))

                           ;; Inject google auth token provider faults conditionally
                           (if should-inject-token-provider?
                             (if-let [fault (maybe-inject-token-provider-fault! rng)]
                               (do
                                 (swap! google-auth-token-provider-faults assoc :get-access-token fault)
                                 (flow/return fault))
                               (flow/return nil))
                             (flow/return nil)))))))

(defn match? [expected actual]
  (let [message (if (empty? @*injected-faults*)
                  "\u001B[35mNo faults were injected.\u001B[0m"
                  (str "\033[1;35m\n\nFailure injection: \u001B[0m\u001B[35m The following failures were injected:\n"
                       (with-out-str (clojure.pprint/pprint @*injected-faults*))
                       "\u001B[0m\n"))]
    (flow/flow (str message)
          (state-flow.matchers/match? expected actual))))

(defmacro match-case?
  [response & clauses]
  `(let [faults-injected?# @common-test/*injected-faults*
         partitions# (partition 2 '~clauses)
         no-faults-injected-clause# (filter (fn [[cond#]] (= :no-faults-injected cond#)) partitions#)
         else-clause# (filter (fn [[cond#]] (= :else cond#)) partitions#)]

     (cond
       (not= (count no-faults-injected-clause#) 1)
       (throw (IllegalArgumentException. "match-case? expects exactly one :no-faults-injected clause"))

       (> (count else-clause#) 1)
       (throw (IllegalArgumentException. "match-case? can only have one :else clause"))

       (empty? faults-injected?#)
       ~(let [[_ matcher] (first (filter (fn [[cond]] (= :no-faults-injected cond)) (partition 2 clauses)))]
          `(match? ~matcher ~response))

       :else
       ~(let [other-clauses (filter (fn [[cond]] (and (not= cond :no-faults-injected) (not= cond :else))) (partition 2 clauses))
              else-clause (first (filter (fn [[cond]] (= :else cond)) (partition 2 clauses)))]
          `(cond
             ~@(mapcat (fn [[condition matcher]]
                         [condition `(match? ~matcher ~response)])
                       other-clauses)
             ~@(when else-clause
                 [:else `(match? ~(second else-clause) ~response)]))))))

(defn indicates-matches?
  [expected actual]
  (matcher-combinators.core/indicates-match?
   (matcher-combinators.core/match expected actual)))

(defn http-fault-injected-to?
  ([url method]
   (-> @*injected-faults* (get-in [url method]) some?))
  ([url method expected]
   (indicates-matches? expected (get-in @*injected-faults* [url method]))))

(defn fault-injected-to-google-auth-token-provider?
  ([]
   (-> @*injected-faults* (get :google-auth-token-provider) some?))
  ([fault-type]
   (= fault-type (-> @*injected-faults* (get :google-auth-token-provider) :type))))

(defn request-made-to? [url method]
  (flow/flow "request-made-to?>"
    [requests-log (flow/get-state (comp :*requests-log* :http-client :system))]
    (-> @requests-log (get-in [url method]) some? flow/return)))
