(ns relayer.core
  (:require [taoensso.carmine :as car :refer (wcar)]
            [clojure.string :as string]))

(defn read-tile [tile] (string/split tile #"/"))
(defn write-tile [x y p] (str x "/" y "/" p))

(defn irange [a b]
  "Inclusive range"
  (range a (inc b)))

(defn tiles-between [target-x1 target-y1 target-x2 target-y2]
  "Generates a sequence of tiles on a 2d grid sorted by sum of distances to (x1,y1) and (x2,y2)"
  (assert (< target-x1 target-x2))
  (assert (< target-y1 target-y2))
  (let [target-width  (- target-x2 target-x1)
        target-height (- target-y2 target-y1)]
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