(ns relayer.core
  (:require [taoensso.carmine :as car :refer (wcar)]
            [clojure.string :as string]
            [relayer.config :refer :all]))

(defn read-bracket [tile] (map read-string (string/split tile #"/")))
(defn write-bracket [[x y p]] (str x "/" y "/" p))

(defn irange [a b]
  "Inclusive range"
  (range a (inc b)))

(defn tiles-between [target-x1 target-y1 target-x2 target-y2]
  "Generates a sequence of tiles on a 2d grid sorted by sum of distances to (x1,y1) and (x2,y2)"
  (assert (not (> target-x1 target-x2)))
  (assert (not (> target-y1 target-y2)))
  (let [target-width  (inc (- target-x2 target-x1)) ; always nonzero
        target-height (inc (- target-y2 target-y1))]
    (letfn [(expand [x1 y1 x2 y2 width height]
              (if (< (/ width target-width) (/ height target-height))
                (lazy-cat
                 (for [y (irange y1 y2) tile [[(dec x1) y] [(inc x1) y]]] tile)
                 (expand (dec x1) y1 (inc x2) y2 (inc width) height))
                (lazy-cat
                 (for [x (irange x1 x2) tile [[x (dec y1)] [x (inc y2)]]] tile)
                 (expand x1 (dec y1) x2 (inc y2) width (inc height)))))]
      (let [center-x (/ (+ target-x2 target-x1) 2)
            center-y (/ (+ target-y2 target-y1) 2)
            intitial-x1 (int (Math/floor center-x)) 
            intitial-y1 (int (Math/floor center-y))
            intitial-x2 (int (Math/ceil  center-x))
            intitial-y2 (int (Math/ceil  center-y))]
        (lazy-cat
         (distinct [[intitial-x1 intitial-y1]
                    [intitial-x1 intitial-y2]
                    [intitial-x2 intitial-y1]
                    [intitial-x2 intitial-y2]])
         (expand intitial-x1 intitial-x2 intitial-y1 intitial-y2
                 (- intitial-x2 intitial-x1) (- intitial-y2 intitial-y1)))))))

(defn weighted-intercat [outer-step inner-step colls] 
  "Something in between lazy-cat and interleave. Lazy."
  ; That's the only reasonable way of concating an infinite sequence of infinite sequences
  (letfn 
    [(intercat [first-colls rest-colls]
       (if (or (not-empty first-colls) (not-empty rest-colls))
         (lazy-cat
          (mapcat (partial take inner-step) first-colls) 
          (intercat 
            (keep not-empty (concat (map (partial drop inner-step) first-colls) 
                                    (take outer-step rest-colls))) 
            (drop outer-step rest-colls)))
          []))]
    (intercat [] colls)))

(defn brackets-between [city1 city2]
  (let [bracket1 (-> city1 :bracket read-bracket)
        bracket2 (-> city2 :bracket read-bracket)
        [x1 y1 _] (map min bracket1 bracket2)
        [x2 y2 _] (map max bracket1 bracket2)
        target-width  (inc (- x2 x1)) ; always nonzero
        target-height (inc (- y2 y1))]
    (map write-bracket
      (weighted-intercat (* target-width target-height) 1 
        (map (fn [tile] (map (partial conj tile) population-hierarchy)) 
             (tiles-between x1 y1 x2 y2))))))

(defn cities-in-bracket [bracket]
  (let [city-ids (wcar redis-conn (car/get (str "bracket:" bracket)))]
    (wcar redis-conn :as-pipeline
      (mapv #(car/get (str "city:" %)) city-ids))))

(defn query-cities-between [city1 city2]
  (for [bracket (brackets-between city1 city2)
        city (cities-in-bracket bracket)]
    city))

(def cities-between (memoize query-cities-between))

(defn city [query]
  (if (integer? query) 
    (wcar redis-conn (car/get (str "city:" query)))
    (let [id (read-string (wcar redis-conn (car/get (str "name:" query))))]
      (city id))))