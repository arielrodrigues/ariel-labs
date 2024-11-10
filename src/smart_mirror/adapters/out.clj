(ns smart-mirror.adapters.out
  (:require [clojure.spec.alpha :as s]
            [smart-mirror.specs.out :as out]
            [smart-mirror.weather :as weather]))

(defn- ->unamespaced
  [m]
  (update-keys m (comp keyword name)))

(s/fdef weather-forecast->wire
  :args (s/cat :forecast ::weather/forecast)
  :ret  ::out/weather-forecast)
(defn weather-forecast->wire
  [{::weather/keys [latitude longitude elevation current hourly daily]}]
  {:latitude latitude
   :longitude longitude
   :elevation elevation
   :current (->unamespaced current)
   :hourly (->unamespaced hourly)
   :daily (->unamespaced daily)
   :default-units weather/default-units})
