(ns common.protocols)

(defprotocol HttpClient
  (req! [req-map] "Dispatch an async HTTP request."))
