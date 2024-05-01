(ns tools.sequencer.core
  (:require [clojure.string :as str]
            [re-frame.core  :as rf]
            [mpt.subs       :as subs]
            [mpt.db :refer [default-sequencer-options]]))

(def roman-numerals ["-" "I" "II" "III" "IV" "V" "VI" "VII" "VIII" "IX" "X" "XI" "XII" "XIII" "XIV" "XV"])

;; this allows options argument to be incomplete, ie. not containing all the keys needed
(defn- or-default
  [options]
  (merge default-sequencer-options options))

(defn- shuffle-range
  [options]
  (->> (range (options :min) (inc (options :max)))
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
        (let [full-range  (doall (range (options :min) (inc (options :max)))) ;; is doall needed?
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
     (str/join (options :delimiter) (cond->> result
                                     @(rf/subscribe [::subs/config-boolean :use-roman-numerals?])
                                      (map #(roman-numerals %))
                                      true ;; cast to str in case the next function needs to be run
                                      (map str) ;; (it fails on numbers)
                                     @(rf/subscribe [::subs/config-boolean :vary-roman-case?])
                                      (map #((rand-nth [str/upper-case str/lower-case]) %)))))))