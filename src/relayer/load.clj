(ns relayer.load
  (:require [clojure.java.io :as io]
            [taoensso.carmine :as car :refer (wcar)]))

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

(def geo-zoom 64)
(def population-zoom 0.5)

(defn tile [city]
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
    (map (comp #(assoc % :tile (tile %))
               (fn [m] (reduce #(update %1 %2 read-string) 
                               m 
                               [:geonameid :population :longitude :latitude]))
               #(update % :alternatenames (fn [s] (clojure.string/split s #",")))
               mapify-geoname
               #(clojure.string/split % #"\t")))))

(def redis-conn (some->> (System/getenv "REDIS_URL") (hash-map :uri) (hash-map :spec)))

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

; A slightly opinionated list of
(def european-countries #{ "AL";bania
                           "AD";orra
                           "AT";stria
                           "BY";elarus
                           "BE";lgium
                           "BA";osnia and Herzegovina
                           "BG";ulgaria
                           "HR";oatia
                           "CZ";ech Republic
                           "DK";enmark
                           "EE";stonia
                           "FI";nland
                           "FR";ance
                           "DE";utschland
                           "GI";braltar
                           "HU";ngary
                           "IE";rland
                           "IM";sle of Man
                           "IT";aly
                           "LV";atvia
                           "LI";echtenstein
                           "LT";ituania
                           "LU";xemburg
                           "MK";edonia
                           "MT";alta
                           "ME";ntenegro
                           "NL";etherlands
                           "NO";rway
                           "PL";and
                           "PT";ortugal
                           "RO";mania
                           "RU";ssia
                           "SM";an Marino
                           "RS";erpska
                           "SK";lovakia
                           "SI";lovenia
                           "ES";pania
                           "SE";den
                           "CH";Switzerland
                           "UA";kraine
                           "GB";Great Britain
})

(defn in-europe [city]
  (contains? european-countries (:country-code city)))

(defn id-lookup [cities] (mapify cities :geonameid identity))
(defn name-lookup [cities] (mapify cities :name :geonameid))
(defn tile-lookup [cities] 
  (apply merge-with into (map #(hash-map (:tile %) [(:geonameid %)]) cities)))

(defn load! []
  (let [cities (filter in-europe (initialize-cities "cities15000.txt"))]
    [(into-redis! "city" (id-lookup cities))
     (into-redis! "name" (name-lookup cities))
     (into-redis! "tile" (tile-lookup cities))]))