(ns common.protocols.config)

(defprotocol Config
  (read-value [instance key] "Read config value."))
