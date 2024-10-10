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
