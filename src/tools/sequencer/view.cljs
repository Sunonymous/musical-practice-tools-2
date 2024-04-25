(ns tools.sequencer.view
  (:require
   [tools.sequencer.core :refer [generate!]]
   [kushi.core     :refer [sx]]
   [clojure.string :as str]
   [reagent.core   :as r]))

;; TODO need a way of constraining configuration values during change
;; this does not apply to the functionality of the sequencer itself, though needs to be monitored during creation of the UI
;; min must be greater than 0 and less than max
;; max must be greater than min and less than some pre-defined ceiling
;; length must be greater than 0 and less than some pre-defined ceiling
;; length also depends on duplicates, as if all numbers are unique, the length is
;;   less than or equal to the length of the sequence from min to max
;; dups must be greater than 0 and less than or equal to the length of the sequence

(defn sequencer []
  (let [nums (r/atom nil)
        options (r/atom {:min 4 :max 12 :len 6 :dups 1})]
    (fn []
      [:div
       (sx :.full-rounded
           :bgc--white
           :p--1.5rem)
       [:div
        "sequencer"]
       [:pre (str/join ", " @nums)]
       [:button
        (sx :mb--1rem
            :.full-rounded
            :.filled
            :.positive
            :p--0.5rem
            {:on-click (fn [_] (reset! nums (generate! @options)))})
        "Generate"]])))