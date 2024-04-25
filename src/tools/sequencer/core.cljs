(ns tools.sequencer.core
  (:require [clojure.string :as str]))

;; Sequencer generates sequences with the following responsibilities:

(def default-options
  {:delimiter ", " ;; string to separate number
   :min       1    ;; lowest possible value
   :max       8    ;; highest possible value
   :len       4    ;; length of sequence
   :sort      nil  ;; sort may be < or >     ;; TODO implement sorting
   :dups      1})  ;; dups key is the limit of duplicate nums in the sequence.
                   ;; dups of 1 means each number is unique, 2 means that each number
                   ;;   may appear twice, and so on

;; this allows options argument to be incomplete, ie. not containing all the keys needed
(defn- or-default
  [options]
  (merge default-options options))

;; need function to calculate potential length of sequence range * duplicates allowed
(defn highest-potential-length
  [options]
  42) ;; TODO implement

(defn rand-int-between
  [min max] ;; exclusive max
  (+ min (rand-int max)))

(defn- shuffle-range
  [options]
  (->> (range (options :min) (options :max))
       shuffle
       (take (options :len))))

;; Helper function in use of constructing random sequences.
(defn- at-limit?
  [freqs limit n]
  (<= limit (freqs n)))

(defn- build-sequence
  [options]
    (loop [next-sq []]
      (if (= (options :len) (count next-sq))
        next-sq
        (let [full-range  (doall (range (options :min) (options :max))) ;; is doall needed?
              frequencies (frequencies next-sq)
              possibles   (remove #(at-limit? frequencies (options :dups) %) full-range)
              next-number (rand-nth possibles)]
          (recur (conj next-sq next-number))))))

;; The public function used to generate a sequence of numbers.
(defn generate!
  ([] (generate! default-options))
  ([options]
   (let [full-opts (or-default options)
         result (if (= 1 (:dups options))
                  (shuffle-range  full-opts)
                  (build-sequence full-opts))]
     (str/join (options :delimiter) result))))