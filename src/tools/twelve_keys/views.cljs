(ns tools.twelve-keys.views
  (:require
   [kushi.core :refer [sx]]
   [reagent.core :as r]
   ))

;; Default Key Maps
;; Could construct these programmatically, though is it really worth the effort?
(defonce default-keys-with-flats (into (sorted-map) {"A" false "B♭" false "B" false "C" false "D♭" false "D" false "E♭" false "E" false "F" false "G♭" false "G" false "A♭" false}))
(defonce default-keys-with-sharps (into (sorted-map) {"A" false "A♯" false "B" false "C" false "C♯" false "D" false "D♯" false "E" false "F" false "F♯" false "G" false "G♯" false}))

(defn twelve-keys []
  (let [state (r/atom default-keys-with-flats)]
    (fn []
      [:div
       (sx :.full-rounded
           :bgc--white
           :mb--1rem
           :p--1.5rem)
       "twelve-keys"
       [:div
        (sx :d--f)
        (for [[k on?] @state]
          [:button
           (sx :w--2.5rem
               :m--0.25rem
               :p--0.5rem
               :.full-slight-rounded
               (if on? :.warning :.neutral)
               :ta--c
               {:key k :on-click (fn [_] (swap! state update k not))})
           k])]]))
  )