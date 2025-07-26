(ns common.exceptions
  (:require [clojure.data.json :as json]))

(defn not-found [msg & args]
  (throw (ex-info msg
                  (merge
                   {:exception-type :not-found}
                   args))))

(defn bad-request [msg & args]
  (throw (ex-info (json/write-str {:message msg})
                  (merge
                   {:exception-type :bad-request}
                   args))))

(defn bad-gateway [msg & args]
  (throw (ex-info (json/write-str {:message msg})
                  (merge
                   {:exception-type :bad-gateway}
                   args))))

(defn unauthorized [msg & args]
  (throw (ex-info (json/write-str {:message msg})
                  (merge
                   {:exception-type :unauthorized}
                   args))))

(defn forbidden [msg & args]
  (throw (ex-info (json/write-str {:message msg})
                  (merge
                   {:exception-type :forbidden}
                   args))))

(defn timeout [msg & args]
  (throw (ex-info (json/write-str {:message msg})
                  (merge
                   {:exception-type :timeout}
                   args))))

(defn internal-server-error [msg & args]
  (throw (ex-info (json/write-str {:message msg})
                  (merge
                   {:exception-type :internal-server-error}
                   args))))
