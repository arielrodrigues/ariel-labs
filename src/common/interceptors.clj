(ns common.interceptors
  (:require [clojure.spec.alpha :as s]))

(s/def ::name (s/nilable keyword?))
(s/def ::enter (s/nilable ifn?))
(s/def ::leave (s/nilable ifn?))
(s/def ::error (s/nilable ifn?))

(s/def ::interceptor (s/keys :req-un [::name
                                      ::enter
                                      ::leave
                                      ::error]))

(s/def ::interceptors (s/coll-of ::interceptor))
