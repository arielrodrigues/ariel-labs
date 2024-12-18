(ns common.system
  (:require [com.stuartsierra.component :as component]))

(def system (atom nil))

(defn stop-system! []
  (swap! system component/stop-system))

(defn start-system! []
  (swap! system component/start-system))

(defn bootstrap! [system-map]
  (->> system-map
       component/start-system
       (reset! system))
  @system)

(defn get-component! [name]
  (or (some-> system deref (get name))
      (throw (ex-info "Component not found"
                      {:from      ::get-component!
                       :component name}))))
