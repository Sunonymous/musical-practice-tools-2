(ns tools.sequencer.core
  (:require [clojure.string :as str]
            [mpt.db :refer [default-sequencer-options]]))


;; this allows options argument to be incomplete, ie. not containing all the keys needed
(defn- or-default
  [options]
  (merge default-sequencer-options options))

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
  ([] (generate! default-sequencer-options))
  ([options]
   (let [full-opts (or-default options)
         result (if (= 1 (:dups options))
                  (shuffle-range  full-opts)
                  (build-sequence full-opts))]
     (str/join (options :delimiter) result))))