(ns smart-mirror.controller
  (:require [common.exceptions]
            [smart-mirror.logic :as logic]
            [smart-mirror.time :as time]))

(defn foo
  [_request
   _context]
  (logic/foo))

(defn now
  [{:keys [query-params] :as _request}
   as-of]
  (if-let [timezones (time/qs->timezones (get query-params :include ""))]
    (time/time+zones as-of timezones)
    (common.exceptions/bad-request "invalid timezone")))
