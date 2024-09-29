(ns smart-mirror.controller
  (:require [smart-mirror.logic :as logic]
            [smart-mirror.time :as time]))

(defn foo
  [_request
   _context]
  (logic/foo))

(defn now
  [_request
   as-of]
  (time/->time as-of))
