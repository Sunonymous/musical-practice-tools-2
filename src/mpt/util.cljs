(ns mpt.util
  (:require [mpt.db         :refer [musical-keys-with-flats musical-keys-with-sharps]]
            [clojure.string :refer [join]]))

(defn next-index
  "Given a sequence and an index, provides a random index
   in the sequence, different than the index provided."
  [sq idx]
  (->> sq
     (map-indexed (fn [i _] i))
     (remove #(= idx %))
       shuffle
       first))

(defn is-musical-key? [s]
  (some #{s} (concat musical-keys-with-flats
                     musical-keys-with-sharps)))

(defn values->str
  "Function to format the set of values as a string.
   Used in the input which sets new values."
  ([values] (values->str ", " values))
  ([delimiter values]
   (join delimiter values)))

(defn max-possible-length
  "This function is used to calculate the maximum possible length of
   a given sequence, using its generation parameters."
  [options]
  (let [minn (options :min)
        maxx (options :max) ;; bad idea to shadow functions with min/max...
        dups (options :dups)]
     (count (mapcat #(repeat dups %) (range minn (inc maxx))))))

(defn clamp
  "Takes a value, a minimum value, and a maximum value and returns the
   minimum if the value is lower than the minimum, and the maximum if
   the value is higher than the maximum, otherwise the value itself."
  [minn maxx n]
  (cond
    (<= minn n maxx)  n
    :otherwise        (if (< n minn)
                        minn
                        maxx)))

(defn if-empty-then
  [alt s]
  (if (empty? s)
    alt
    s))

