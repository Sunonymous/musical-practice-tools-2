(ns mpt.browser
  (:require
   ;; Require various functions and macros from kushi.core
   [kushi.core :refer [sx add-google-font!]     ]
   [kushi.ui.button.core    :refer [button]     ]
   [kushi.ui.icon.core      :refer [icon]       ]
   [tools.sequencer.view    :refer [sequencer]  ]
   [tools.toggler.view      :refer [toggler]    ]
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
             :ff--Inter|sans-serif)
    [:p "1-2-3-4"]
    [:p "Ascending"]
    [:p "Ab"]
   ]
  ])

(defn control-buttons
  "These buttons rest at the bottom of the screen
   and control the metronome and new generation."
  []
  [:div (sx
            :.pill
            :mb--1rem
            :pi--2rem
            :pb--1rem
            :bgc--white)
   [button ;; Toggle Metronome
    (sx :.filled :.pill :.xlarge :.semi-bold
        {:on-click (fn [_] (metronome/play))})
    [icon (if (@metronome/state :isPlaying) :pause :play-arrow) ]]
  ])

(defn main-view []
  [:div
   (sx :d--flex
       :w--100%
       :.flex-col-c
       :ai--c)
   [display-panel]
   [sequencer]
   [toggler ["Ascending" "Descending"]]
   [twelve-keys]
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