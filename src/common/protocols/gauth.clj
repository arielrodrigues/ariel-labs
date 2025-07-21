(ns common.protocols.gauth)

(defprotocol TokenProvider
  (get-access-token [this config] "Get a valid access token"))