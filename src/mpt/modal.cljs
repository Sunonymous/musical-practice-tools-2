(ns mpt.modal
    (:require
     [kushi.ui.modal.core        :refer [modal modal-close-button
                                         open-kushi-modal close-kushi-modal]]
     [kushi.core                 :refer [sx    ]]
     [kushi.ui.input.text.core   :refer [input ]]
     [kushi.ui.input.switch.core :refer [switch]]
     [kushi.ui.button.core       :refer [button]]
     [kushi.ui.icon.core         :refer [icon  ]]
     [kushi.ui.input.slider.core :refer [slider]]
     [tools.twelve-keys.views    :as twelve-keys]
     [tools.expression.core      :as expression]
     [tools.toggler.core         :as toggler]
     [mpt.events    :as events]
     [mpt.util      :as util]
     [mpt.subs      :as subs]
     [reagent.dom   :as rdom]
     [re-frame.core :as rf]
     [reagent.core  :as r]))

(def toolkw->modal-title
  {:sequencer  "Sequencer Settings"
   :toggler    "Toggler Settings"
   :key        "Musical Key Settings"
   :expression "Expression Settings"})

;; Individual Input Components

(defn switch-toggle
  [label sub-kw config-kw]
  [:div (sx :.flex-row-c :gap--1rem )
   [:label label]
   [switch (sx {:-on?      @(rf/subscribe [::subs/config-boolean sub-kw])
                :on-click #(rf/dispatch  [::events/toggle-boolean-config config-kw])})]])

(defn numeric-input
  [label minimum maximum db-path]
  [input (sx {:value @(rf/subscribe [::subs/nested-value db-path])
              :type :number :min minimum :max maximum
              :-label label
              :on-change #(rf/dispatch [::events/set-numeric-config db-path (-> % .-target .-value)])})])

(defn string-input
  "This component allows configuration of a string value. It is a form-2 component
   because it only alters its value on blur rather than on change."
  [label db-path]
  (let [user-str (r/atom @(rf/subscribe [::subs/nested-value db-path]))]
    (fn [label db-path]
      [input (sx
              {:value     @user-str
               :-label     label
               :on-blur   #(rf/dispatch [::events/set-string-config db-path @user-str])
               :on-change #(reset! user-str (-> % .-target .-value))})])))

;; had bugs when trying to use this. leaving, just in case
(defn slider-input
  [minn maxx db-path]
  [:div (sx)
   [slider {:min minn :max maxx :-step-marker :label
            :value     @(rf/subscribe [::subs/nested-value db-path])
            :on-change #(rf/dispatch  [::events/set-numeric-config db-path (-> % .-target .-value)])}]])

(def setting->title
  {:expression-source [:h3 (sx :.bold) "Change Attribute:"]
   :toggler-values    [:h3 (sx :.bold) "Values to select from:"]
   :key-editor        [:h3 (sx :.bold) "Key Editor"]
   :use-sharps?         nil ;; If nil, has its own label
   :alert-on-complete?  nil
   :sequencer-min-max   nil
   :sequencer-len-dups  nil
   :sequencer-delimiter nil})

(defn values-config-component
  []
  (let [values  @(rf/subscribe [::subs/toggler-values])
        user-str (r/atom (util/values->str values))]
    (fn [] [input
            (sx
             {:value     @user-str
              :-helper   "Add comma-separated options to this box to alternate between."
              :on-blur   #(rf/dispatch [::events/set-toggler-values @user-str])
              :on-change #(reset! user-str (-> % .-target .-value))})])))

;; Component/Complex Composites

(defn sequencer-min-max
  "These components control the minimum and maximum numbers possible in the
   sequence. They go together because they are intrinsically tied, ie. min
   must remain lower than max and max higher than min."
  []
  (let [minn @(rf/subscribe [::subs/nested-value [:config :sequencer :min]])
        maxx @(rf/subscribe [::subs/nested-value [:config :sequencer :max]])]
    [:div (sx :.flex-row-se :ai--c)
     [input (sx {:-label "Min:"
                 :value minn :min 1 :max maxx :type :number
                 :on-change #(rf/dispatch [::events/set-numeric-config [:config :sequencer :min] (-> % .-target .-value)])})]
     [input (sx {:-label "Max:"
                 :value maxx :min minn :max 15 :type :number
                 :on-change #(rf/dispatch [::events/set-numeric-config [:config :sequencer :max] (-> % .-target .-value)])})]]))

(defn expression-source
  []
  (let [show-text? (r/atom false)
        hide-text! #(reset! show-text? false)]
    (fn []
      [:div (sx :mi--auto)
       [:select (sx :b--1px:solid:black :p--0.5rem :.full-rounded
                    {:default-value "" :on-change (fn [e]
                                                    (rf/dispatch [::events/set-expression-source (-> e .-target .-value)])
                                                    (reset! show-text? true)
                                                    (js/setTimeout hide-text! 1750))})
        [:option {:disabled true} ""] ;; instead of trying to sub from db
        (for [source (keys expression/sources)]
          [:option {:key source :value source}
           (expression/source->display source)])]
       [:br]
       (when @show-text?
         [:p (sx :.medium :.oblique {:style {:position :relative :top "1.5rem"}})
          "Expressions updated."])])))

(def setting->component
  {:expression-source   [expression-source]
   :toggler-values      [values-config-component]
   :key-editor          [twelve-keys/key-editor]
   :use-sharps?         [switch-toggle "Use Sharps?" :use-sharps? :use-sharps?]
   :alert-on-complete?  [switch-toggle "Alert when all keys completed?" :alert-on-complete? :alert-on-complete?]
   :sequencer-min-max   [sequencer-min-max]
   :sequencer-delimiter [:div (sx :w--25% :mi--auto)
                         [string-input  "Delimiter:"   [:config :sequencer :delimiter]]]
   :sequencer-len-dups  [:div (sx :.flex-row-se :ai--c)
                         [numeric-input "Length:" 1 10 [:config :sequencer :len]]
                         [numeric-input "Duplicates: " 1 5 [:config :sequencer :dups]]]
   })

(defn setting->input
  "Ties together the title and the component of a particular setting
   keyword and returns the full input hiccup that will change the setting."
  [setting]
    [:<> {:key setting}
     (get setting->title     setting [:p "TITLE NOT FOUND FOR SETTING "     setting])
     (get setting->component setting [:p "COMPONENT NOT FOUND FOR SETTING " setting])])

(def toolkw->settings
  {:sequencer  [:sequencer-min-max :sequencer-len-dups :sequencer-delimiter #_:use-roman-numerals?] ;; TODO re-enable once implemented
   :toggler    [:toggler-values]
   :key        [:key-editor #_:use-sharps? #_:alert-on-complete?] ;; TODO re-enable once implemented
   :expression [:expression-source]})

(defn config
  "Reusable modal base to assist with the configuration of tools and their generations."
  [tool-kw]
  (let [modal-id (str tool-kw "-config")]
    [:div
     [button
      {:on-click #(open-kushi-modal modal-id)}
      [icon :settings]]
     [modal
      (sx
       :border-radius--24px
       :b--2px:solid:$gray-900
       :&_.kushi-modal-description:fs--$small
       {:-modal-title (toolkw->modal-title tool-kw)
        :id modal-id})
      [:div
       (sx :.flex-col-fs :gap--1em)
       (for [setting (toolkw->settings tool-kw)]
         (setting->input setting))]
      [:div
       (sx :.flex-row-fe :gap--1em)
       [button
        (sx :.minimal :.pill {:on-click close-kushi-modal})
        "Close"]
       ]]])
)