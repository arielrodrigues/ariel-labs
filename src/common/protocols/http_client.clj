(ns common.protocols.http-client)

(defprotocol HttpClient
  (req! [instance req-map] "Dispatch an async HTTP request."))
