(ns oauth.gdocs
  "Integration test for access to google docs.
   NOTE:
   This test will open a browser window to sign-in and grant access to token
   Future refactoring to mock http/request needed to avoid manual steps
   but wanted to ensure real thing worked before mocking
   "
   (:require [oauth.client :as oauth] :reload-all)
   (:use [clojure.test])
   (:use [clojure.contrib.javadoc.browse :only [open-url-in-browser]])
   (:use [ring.adapter.jetty :only [run-jetty]])
   (:use [ring.middleware.params :only [wrap-params]])
   (:use [clojure.contrib.str-utils :only [str-join]]))


(def consumer-key )
(def consumer-secret )
(load "google_keys")

(def scope (str-join " " ["https://docs.google.com/feeds/"
                          "https://picasaweb.google.com/data/"]))

(defn create-gdata-consumer [consumer-key consumer-secret]
  (oauth/make-consumer
   consumer-key
   consumer-secret
   "https://www.google.com/accounts/OAuthGetRequestToken"
   "https://www.google.com/accounts/OAuthGetAccessToken"
   "https://www.google.com/accounts/OAuthAuthorizeToken" 
   :hmac-sha1))

(defn request-access-token [callback
                            oauth-consumer
                            token
                            request
                            ]
  (let [{{verifier "oauth_verifier" oauth_token "oauth_token"} :params} request]
       (callback (oauth/access-token oauth-consumer token verifier))))

(defn callback-serv [callback]
  (let [server (promise)
        handler (wrap-params (fn [req]
                               (future (.stop @server))
                               (callback req))
                             {:status 200}                             )]
    (deliver server (run-jetty handler {:port 8080 :join? false})))
    (println "waiting for authorization") )

(defn not-nil? [x] (not (nil? x)))
  
(deftest dance
  (cond (and (not-nil? consumer-key) (not-nil? consumer-secret))
    (let [consumer (create-gdata-consumer consumer-key consumer-secret) 
          token (oauth/request-token consumer :scope scope :oauth_callback "http://localhost:8080/")
          access-token (promise)]
      (callback-serv (partial request-access-token (partial deliver access-token) consumer token))
      (open-url-in-browser (oauth/user-approval-uri consumer token))
    (is (not-nil? @access-token)))
    :else (is false "keys not specified")))
  

  
  
