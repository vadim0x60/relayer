(ns relayer.handler
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer (ok)]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.adapter.jetty :as ring]
            
            [schema.core :as s]

            [relayer.language :refer [Location interval]]
            [relayer.core :refer :all])
    (:gen-class))

(def search-timeout 300000) ; 5 minutes
(def results-timeout 500)

(def searches (atom {}))

(defn search [from to]
  (let [id (str (java.util.UUID/randomUUID))]
    (swap! searches
      #(->> [{:from from
              :to to
              :outbound-interval interval
              :undecided true}]
          (decide-route)
          (future)
          (assoc % id)))
    id))

(defapi app
  { :swagger
    { :ui "/"
      :spec "/swagger.json"
      :data { :info {:title "relayer"   :description ""}
              :tags [{:name "search-api" :description ""}]}}}
  
  (context "/" [] 
    :tags ["search-api"]
    :middleware [#(wrap-cors % #".*")]

    (GET "/between/:a/:b" []
      :summary "Get potential stopovers: big cities on a line between a and b"
      :path-params [a :- s/Str, b :- s/Str]
      :query-params [batch :- s/Int]
      :return s/Any

      (internal-server-error {:reason \"not implemented\"}))))

(defn -main []
  (let [port (Integer. (or (System/getenv "PORT") "8080"))] 
    (ring/run-jetty app {:port port :join? false})))