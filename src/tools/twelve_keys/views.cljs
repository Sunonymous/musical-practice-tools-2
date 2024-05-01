(ns tools.twelve-keys.views
  (:require
   [kushi.ui.input.switch.core :refer [switch]]
   [kushi.ui.button.core       :refer [button]]
   [kushi.core                 :refer [sx]]
   [mpt.events    :as events]
   [mpt.subs      :as subs]
   [re-frame.core :as rf]
   [reagent.core  :as r]))

;; Default Key Maps
;; These were moved into DB to prevent circular depencency.
;; TODO find where they are used and change the references.
(defonce flat-keys ["A" "B♭" "B" "C" "D♭" "D" "E♭" "E" "F" "G♭" "G" "A♭"])
(defonce sharp-keys ["A" "A♯" "B" "C" "C♯" "D" "D♯" "E" "F" "F♯" "G" "G♯" ])
;; TODO create way of toggling to sharps from flats

(defonce keys-with-flats (into (sorted-map) {"A" false "B♭" false "B" false "C" false "D♭" false "D" false "E♭" false "E" false "F" false "G♭" false "G" false "A♭" false}))
(defonce keys-with-sharps (into (sorted-map) {"A" false "A♯" false "B" false "C" false "C♯" false "D" false "D♯" false "E" false "F" false "F♯" false "G" false "G♯" false}))

(defn key-editor
  "Used as a sort of editor to configure which keys can be excluded or selected."
  []
  (let [selected-key   (r/atom nil)]
    (fn []
      (let [current-key   @(rf/subscribe [::subs/key])
            excluded-keys @(rf/subscribe [::subs/excluded-keys])
            seen-keys     @(rf/subscribe [::subs/seen-keys])
            selected-excluded? (excluded-keys @selected-key)
            selected-seen?     (seen-keys @selected-key)]
        [:div
         [:div
          (sx :d--f)
          (for [key flat-keys]
            (let [active?   (= key current-key)
                  excluded? (excluded-keys key)
                  seen?     (seen-keys key)]
              [:button
               (sx :w--2.5rem
                   :m--0.25rem
                   :p--0.5rem
                   :.full-slight-rounded
                   (cond
                     excluded?  :.negative
                     active?    :.warning
                     seen?      :.accent
                     :otherwise :.neutral)
                   :ta--c
                   {:key key
                    :on-click #(reset! selected-key key)
                    :style {:outline (if (= key @selected-key) "2px solid black" "none")}})
               key]))]
         [:p (sx :mb--1rem :.oblique :ta--c) "Selected: " @selected-key]
         [:div (sx :.flex-row-se)
          [button (sx {:on-click #(rf/dispatch [::events/toggle-exclude-key @selected-key])})
           (if selected-excluded? "Include" "Exclude")]
          [button (sx {:on-click #(rf/dispatch [::events/toggle-seen-key @selected-key])})
           (if selected-seen? "Un-complete" "Complete")]
          (when-not selected-excluded?
            [button (sx {:on-click #(rf/dispatch [::events/focus-key @selected-key])})
             "Focus"])
          [button (sx {:on-click #(rf/dispatch [::events/reset-keys])}) "Reset"]]
         ]))))