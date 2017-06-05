(ns relayer.load
  (:require [clojure.java.io :as io]
            [taoensso.carmine :as car :refer (wcar)]
            
            [relayer.config :refer :all]))

(def fields  [:geonameid :name :asciiname :alternatenames
              :latitude :longitude 
              :feature-class :feature-code  
              :country-code :cc2           
              :admin1-code :admin2-code :admin3-code :admin4-code 
              :population    
              :elevation :dem               
              :timezone :modification-date])

(defn mapify-geoname [geoname-vector]
  (apply hash-map (interleave fields geoname-vector)))

(defn mapify [coll key-f value-f] (apply hash-map (interleave (map key-f coll) (map value-f coll))))

(defn bracket [city]
  "Locate the city in a 3D (lat-long-population) grid"
  (let [{:keys [latitude longitude population]} city]
    (str (-> longitude (+ 180) (/ 360) (* geo-zoom) (int)) "/" 
         (let [radians (Math/toRadians latitude)
               tan     (Math/tan radians)
               cos     (Math/cos radians)
               log     (Math/log (+ tan (/ 1 cos)))]
           (int (* geo-zoom (/ (- 1 (/ log Math/PI)) 2)))) "/"
         ; FIXME There are 0 population items in the DB and logarithms don't like that
         ; Temporary solution: put one extra guy (girl) into every item
         (int (* (Math/log (inc population)) population-zoom))))) 

(defn initialize-cities [resource-name]
  (->>
    (io/resource resource-name)
    (io/reader)
    (line-seq)
    (map (comp #(assoc % :bracket (bracket %))
               (fn [m] (reduce #(update %1 %2 read-string) 
                               m 
                               [:geonameid :population :longitude :latitude]))
               #(update % :alternatenames (fn [s] (clojure.string/split s #",")))
               mapify-geoname
               #(clojure.string/split % #"\t")))))

(defn redis-batch [conn coll size f]
  (as-> nil $
    (for [batch (partition-all size coll)
          result (wcar conn (doall (for [elem batch] (f elem))))]
      result)
    (filter #(not= % "OK") $)
    (first $)
    (or $ "OK")))

(defn into-redis! [prefix m]
  (redis-batch redis-conn m 300
    (fn [[key value]] (-> (str prefix ":" key) (car/set value)))))

(defn id-lookup [cities] (mapify cities :geonameid identity))
(defn name-lookup [cities] (mapify cities :name :geonameid))
(defn bracket-lookup [cities] 
  (apply merge-with into (map #(hash-map (:bracket %) [(:geonameid %)]) cities)))

(defn load! []
  (let [cities (filter acceptable-city 
                       (initialize-cities "cities15000.txt"))]
    [(into-redis! "city" (id-lookup cities))
     (into-redis! "name" (name-lookup cities))
     (into-redis! "bracket" (bracket-lookup cities))]))