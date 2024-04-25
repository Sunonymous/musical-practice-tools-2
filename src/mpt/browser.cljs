(ns mpt.browser
  (:require
   ;; Require various functions and macros from kushi.core
   [kushi.core :refer [sx add-google-font!]     ]
   [kushi.ui.card.core      :refer [card]       ]
   [kushi.ui.button.core    :refer [button]     ]
   [kushi.ui.icon.core      :refer [icon]       ]
   [tools.toggler.core      :refer [toggler]    ]
   [tools.twelve-keys.views :refer [twelve-keys]]
   [mpt.metronome        :as metronome]
   [mpt.events           :as events]
   [reagent.dom          :as rdom]
   [mpt.subs             :as subs]
   [re-frame.core        :as rf]
   [reagent.core         :as r]
   [mpt.shared-styles]))

(add-google-font! {:family "Inter"
                   :styles {:normal [400 700]
                            :italic [400 700]}})

;; Application Components and Tools

(defn display-panel
  "The main view of the description of the music
   which should be played. Sits at the top of the screen."
  []
  [:div (sx :w--90%
            :.full-rounded
            :mb--1rem
            :p--2rem
            :bgc--white)
   [:div (sx :.full-slight-rounded
             :b--1px:solid:#efefef
             :pi--2rem
             :pb--4rem
             :d--f :jc--sa
             :c--black
             :ff--Inter|sans-serif
             {:style {:flex-wrap "wrap"}})
    (when @(rf/subscribe [::subs/is-visible? :sequencer])
      [:p @(rf/subscribe [::subs/sequence])])
    (when @(rf/subscribe [::subs/is-visible? :toggler])
      [:p @(rf/subscribe [::subs/toggler])])
    (when @(rf/subscribe [::subs/is-visible? :key])
      [:p @(rf/subscribe [::subs/key])])
   ]
  ])

(defn tool-menu
  "This component is reused for configuration of the various tools
   and generation."
  [title tool-kw]
  [card (sx :w--fit-content :mb--1rem :.rounded)
   [:span (sx :.medium :.bold) title]
   [:div (sx :d--f :pb--0.5rem {:style {:gap "8px"}})
    [button (sx :.filled :.pill :.small :.semi-bold
                {:on-click #(rf/dispatch [::events/toggle-visible tool-kw])})
     [icon (if @(rf/subscribe [::subs/is-visible? tool-kw])
             :visibility
             :visibility-off)]]
    [button (sx :.filled :.pill :.small :.semi-bold) [icon :lock]]
    [button (sx :.filled :.pill :.small :.semi-bold) [icon :autorenew]]]]
  )

(defn toolsbar
  "Contains all the tool-menu components for each individual tool."
  []
  [:div (sx :d--f {:style {:flex-wrap "wrap" :gap "1rem"}})
   [tool-menu "Sequencer" :sequencer]
   [tool-menu "Toggler"   :toggler]
   ])

(defn control-buttons
  "These buttons rest at the bottom of the screen
   and control the metronome and new generation."
  []
  [:div (sx
         :d--f
         :.pill
         :mb--1rem
         :pi--2rem
         :pb--1rem
         :bgc--white
         {:style {:gap "1rem"}})
   [button ;; Toggle Metronome
    (sx :.filled :.pill :.xlarge :.semi-bold
        {:on-click (fn [_] (metronome/play))})
    [icon (if (@metronome/state :isPlaying) :pause :play-arrow)]]
   [button ;; Generate New Data
    (sx :.filled :.pill :.xlarge :.semi-bold
        {:disabled (@metronome/state :isPlaying)
         :on-click (fn [_] (rf/dispatch [::events/generate!]))})
    [icon :autorenew]]])

(defn main-view []
  [:div
   (sx :d--flex
       :w--100%
       :.flex-col-c
       :ai--c)
   [display-panel]
   [toolsbar]
   [twelve-keys]
  ;;  [:pre (sx :c--white) @(rf/subscribe [::subs/full])]
   [:canvas#metrocanvas (sx :d--none)]
   [control-buttons]
  ])

;; start is called by init and after code reloading finishes
(defn ^:dev/after-load start []
  (rdom/render [main-view] (.getElementById js/document "app")))

(defn init []
  (rf/dispatch-sync [::events/initialize-db])

  ;; Initialize Metronome with page's AudioContext
  (.addEventListener js/window "load" #(metronome/init @(rf/subscribe [::subs/audio-context])))

  ;; Ready to go!
  (start))

;; this is called before any code is reloaded
(defn ^:dev/before-load stop [] (js/location.reload))