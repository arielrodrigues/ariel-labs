(ns common.gauth
  (:require [com.stuartsierra.component :as component]
            [common.exceptions]
            [common.protocols.config :as protocols.config]
            [common.protocols.gauth :as protocols.gauth])
  (:import [com.google.auth.oauth2 UserCredentials]))

(defrecord GoogleAuthTokenProvider []
  component/Lifecycle
  protocols.gauth/TokenProvider

  (start [this] this)
  (stop [this] this)

  (get-access-token [_ config]
    (let [client-id (protocols.config/read-value config :gcal-client-id)
          client-secret (protocols.config/read-value config :gcal-client-secret)
          refresh-token (protocols.config/read-value config :gcal-refresh-token)
          creds (UserCredentials/newBuilder)
          _ (.setClientId creds client-id)
          _ (.setClientSecret creds client-secret)
          _ (.setRefreshToken creds refresh-token)
          user-creds (.build creds)]
      (try
        (-> user-creds
            .refreshAccessToken
            .getTokenValue)
        (catch Exception e
          (common.exceptions/unauthorized "Failed to authenticate with Google Calendar API - invalid or expired credentials" e))))))

(defrecord MockTokenProvider [access-token]
  component/Lifecycle
  protocols.gauth/TokenProvider
  
  (start [this] this)
  (stop [this] this)
  
  (get-access-token [_ _]
    access-token))

(defn new-google-auth-token-provider []
  (->GoogleAuthTokenProvider))

(defn new-mock-token-provider [token]
  (->MockTokenProvider token))
