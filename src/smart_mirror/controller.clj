(ns smart-mirror.controller
  (:require [common.exceptions]
            [smart-mirror.logic :as logic]
            [smart-mirror.time :as time]))

(defn foo
  [_request
   _context]
  (logic/foo))

(defn now
  [include as-of]
  (if-let [timezones (time/qs->timezones include)]
    (time/time+zones as-of timezones)
    (common.exceptions/bad-request "invalid timezone")))
