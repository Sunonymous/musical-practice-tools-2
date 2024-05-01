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