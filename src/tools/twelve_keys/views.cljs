(ns tools.twelve-keys.views
  (:require
   [mpt.db :refer [musical-keys-with-flats musical-keys-with-sharps]]
   [kushi.ui.input.switch.core :refer [switch]]
   [kushi.ui.button.core       :refer [button]]
   [kushi.core                 :refer [sx]]
   [mpt.events    :as events]
   [mpt.subs      :as subs]
   [re-frame.core :as rf]
   [reagent.core  :as r]))

(defn key-editor
  "Used as a sort of editor to configure which keys can be excluded or selected."
  []
  (let [
        selected-key (r/atom nil)]
    (fn []
      (let [all-keys     (if @(rf/subscribe [::subs/nested-value [:config :use-sharps?]])
                           musical-keys-with-sharps
                           musical-keys-with-flats)
            current-key   @(rf/subscribe [::subs/key])
            excluded-keys @(rf/subscribe [::subs/excluded-keys])
            seen-keys     @(rf/subscribe [::subs/seen-keys])
            selected-excluded? (excluded-keys @selected-key)
            selected-seen?     (seen-keys @selected-key)]
        [:div
         [:div
          (sx :d--f)
          (for [key all-keys]
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