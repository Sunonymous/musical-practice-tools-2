(ns tools.toggler.view
  (:require
   [kushi.core     :refer [sx]]
   [reagent.core :as r]))

(defonce default-toggler-values ["On" "Off"])

(defn- any-other-index [sq idx]
  (->> sq
     (map-indexed (fn [i _] i))
     (remove #(= idx %))
       shuffle
       first))

(defn toggler [values]
  (let [active?     (r/atom nil)
        on-idx      (r/atom 0)
        interval-id (r/atom nil)]
    (fn []
      [:div
       (sx :.full-rounded
           :bgc--white
           :mb--1rem
           :p--1.5rem)
       [:div
        "toggler"]
       (if @active?
         [:pre (nth values @on-idx "Oops! Something went wrong.")]
         [:pre (sx :c--gray) "-Off-"])
       [:button
        (sx :mb--1rem
            :.full-rounded
            :.filled
            :.positive
            :p--0.5rem
            {:on-click
             (fn [_]
               (if @active?
                 (js/clearInterval @interval-id)
                 (reset! interval-id (js/setInterval #(swap! on-idx (partial any-other-index values)) 1000)))
               (swap! active? not))})
        (if @active? "Deactivate" "Activate")]])))
