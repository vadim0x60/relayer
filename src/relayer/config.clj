(ns relayer.config)

; Affects how many tiles the map is divided into
(def geo-zoom 64)

; Affects how many population brackets the cities are divided into
(def population-zoom 0.5)
(def population-hierarchy [8 7 6 5 4])
; Note that population-zoom and population-hierarchy are linked
; I got hierarchy experimentally (all the cities fell into brackets 4-8),
; so if you change zoom, you'll have to redo the experiment

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

; Yeah, that gives us European Sakhalin
; But Asian St. Petersburg would be worse
; And for transport applications it is generally a good idea to divide the map by country
(defn in-europe? [city]
  (contains? european-countries (:country-code city)))

(defn acceptable-city [city]
  (and (in-europe? city) ; Main relayer server only supports Europe
                         ; Remove to support entire world
       (-> city :feature-code (not= "PPLX")))) ; PPLX = big unit within a city

; 3 options supported:
; - launch redis locally with default port and don't touch the code below
; - "export REDIS_URL=YOUR_REDIS_URL" and don't touch the code below
; - change the code below to your redis config
(def redis-conn (some->> (System/getenv "REDIS_URL") (hash-map :uri) (hash-map :spec)))

; ID format
(defn city-id [city] (format "%d$%d" (-> city :latitude  (* 100) (int))
                                     (-> city :longitude (* 100) (int))))

; An opinionated list of
(def important-keys [:name :timezone :country-code :latitude :longitude :id :bracket])
; The /between search will only return these fields in city objects