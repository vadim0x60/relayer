(ns relayer.handler
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer (ok not-found)]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.adapter.jetty :as ring]
            
            [schema.core :as s]

            [relayer.core :refer :all])
    (:gen-class))

(def last-reload (atom nil))

(defapi app
  { :swagger
    { :ui "/"
      :spec "/swagger.json"
      :data { :info {:title "relayer"   :description ""}
              :tags [{:name "search-api" :description ""}]}}}
  
  (context "/" [] 
    :tags ["search-api"]
    :middleware [#(wrap-cors % #".*")]

    (GET "/city/:query" []
      :summary "Get city details"
      :path-params [query :- s/Str]

      (if-let [c (city query)]
        (ok c)
        (not-found)))

    (GET "/between/:a/:b" []
      :summary "Get potential stopovers: big cities on a line between a and b"
      :path-params [a :- s/Str, b :- s/Str]
      :query-params [{skip :- s/Int 0} {limit :- s/Int 20}]
      :return s/Any

      (let [city1 (city a) city2 (city b)]
        (if (and city1 city2)
          (->> (cities-between city1 city2)
               (drop skip)
               (take limit)
               (ok))
          (not-found))))))

(defn -main []
  (let [port (Integer. (or (System/getenv "PORT") "8080"))] 
    (ring/run-jetty app {:port port :join? false})))