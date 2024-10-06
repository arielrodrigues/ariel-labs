(ns common.exceptions)

(defn not-found [msg & args]
  (throw (ex-info msg
                  (merge
                   {:exception-type :not-found}
                   args))))

(defn bad-request [msg & args]
  (throw (ex-info msg
                  (merge
                   {:exception-type :bad-request}
                   args))))
