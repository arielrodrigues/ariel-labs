(ns common.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [common.protocols.config :as protocols]))

(defn log-error
  [& args]
  (binding [*out* *err*]
    (apply printf args)))

(def project-path (System/getProperty "user.dir"))

(defrecord Config [filename]
  protocols/Config
  (read-value [instance key]
    (-> instance :config key))

  component/Lifecycle
  (start [component]
    (try
      (with-open [reader (io/reader filename)]
        (->> (java.io.PushbackReader. reader)
             edn/read
             (assoc component :config)))
      (catch java.io.IOException e
        (log-error "Couldn't open '%s': %s\n" filename (.getMessage e)))

      (catch RuntimeException e
        (log-error "Error parsing edn file '%s': %s\n" filename (.getMessage e)))))

  (stop [component]
    (assoc component :config nil)))

(defn new-config
  ([]
   (new-config (str project-path "/config.edn")))
  ([filepath]
   (map->Config {:filename filepath})))
