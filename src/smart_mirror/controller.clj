(ns smart-mirror.controller
  (:require [smart-mirror.logic :as logic]))

(defn foo
  [_request
   _context]
  (logic/foo))
