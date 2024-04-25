(ns sequencer-test
  (:require
   [cljs.test :refer [deftest is testing]]
   [tools.sequencer.core :refer [generate!]]))

;; Minimum number
(deftest test-min
  (testing "Sequencer can set a minimum number."
    (let [minimum 2
          sq (generate! {:min minimum})]
      (is (>= (apply min sq) minimum)))))

;; Maximum number
(deftest test-max
  (testing "Sequencer can set a maximum number."
    (let [maximum 7
          sq      (generate! {:max maximum})]
      (is (<= (apply max sq) maximum)))))

;; Length of Sequence
(deftest test-length
  (testing "Sequencer can set a length for a sequence."
    (let [length 12
          sq     (generate! {:len length :max 42})]
      (is (= (count sq) length)))))

;; Sequence as Shuffled Range (all unique)
(deftest test-range
  (testing "Sequencer returns a range of numbers when duplicates limited to one."
    (let [length 6
          sq     (generate! {:dups 1 :len length})]
      (is (= (count sq) (count (set sq)))))))

;; Limit of Duplicate Numbers
(deftest test-duplicate-limit
  (testing "Sequencer returns numbers with less than a certain number of duplicates."
    (let [dup-limit 4
          sq        (generate! {:dups dup-limit :len 15 :max 50})
          freq      (frequencies sq)]
      (is (every? #(<= % dup-limit) (vals freq))))))
