(ns mpt.browser
  (:require
   ;; Require various functions and macros from kushi.core
   [kushi.core :refer [sx add-google-font! inject! merge-attrs]]
   [kushi.ui.card.core         :refer [card]         ]
   [kushi.ui.button.core       :refer [button]       ]
   [kushi.ui.icon.core         :refer [icon]         ]
   [kushi.ui.input.switch.core :refer [switch]       ]
   [kushi.ui.tooltip.core      :refer [tooltip-attrs]]
   [mpt.metronome        :as metronome]
   [mpt.events           :as events]
   [mpt.modal            :as modal]
   [reagent.dom          :as rdom]
   [mpt.subs             :as subs]
   [re-frame.core        :as rf]
   [reagent.core         :as r]
   [clojure.pprint :as pp]
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
             {:style {:flex-wrap   :wrap
                      :white-space :pre}})
    (when @(rf/subscribe [::subs/is-visible? :sequencer])
      [:p (sx :.display-text) @(rf/subscribe [::subs/sequence])])
    (when @(rf/subscribe [::subs/is-visible? :toggler])
      [:p (sx :.display-text) @(rf/subscribe [::subs/toggler])])
    (when @(rf/subscribe [::subs/is-visible? :key])
      [:p (sx :.display-text) @(rf/subscribe [::subs/key])])
    (when @(rf/subscribe [::subs/is-visible? :expression])
      [:p (sx :.display-text) @(rf/subscribe [::subs/expression])])]
   (let [remaining-beats @(rf/subscribe [::subs/remaining-beats])]
     [:p (sx :p--relative :pb--0.25rem :.small :.oblique :ta--right
             {:style {:visibility (if (and (@metronome/state :isPlaying)
                                           (seq @(rf/subscribe [::subs/synced-tools])))
                                    :visible :hidden)}})
      (if (= remaining-beats 1)
        "New generation on next beat."
        (str "New generation in " remaining-beats " beats."))])])

(def tools->gen-event
  {:sequencer  ::events/next-sequence
   :toggler    ::events/next-toggle
   :key        ::events/next-key
   :expression ::events/next-expression})

(defn tool-menu
  "This component allows configuration of the various tools and generation."
  [title tool-kw]
  (let [visible? @(rf/subscribe [::subs/is-visible? tool-kw])
        locked?  @(rf/subscribe [::subs/is-locked?  tool-kw])
        synced?  @(rf/subscribe [::subs/is-synced?  tool-kw])]
    [card (sx :w--fit-content :mb--1rem :.rounded)
     [:span (sx :.medium :.bold) title]
     [:div (sx :d--f :pb--0.5rem {:style {:gap "8px"}})
      [button (sx :.toolmenu-button (when visible? :.filled)
                  {:on-click #(rf/dispatch [::events/toggle-tool-attribute :show tool-kw])
                   :aria-label (str "Show or Hide " title)})
       [icon (if visible? :visibility :visibility-off)]]
      [button (sx :.toolmenu-button (when locked? :.filled)
                  {:on-click #(rf/dispatch [::events/toggle-tool-attribute :lock tool-kw])
                   :aria-label (str "Lock " title)})
       [icon (if locked? :lock :lock-open)]]
      [button (sx :.toolmenu-button
                  {:on-click #(rf/dispatch [(tools->gen-event tool-kw)])
                   :aria-label (str "Generate " title)})
       [icon :autorenew]]
      [button (sx :.toolmenu-button (when synced? :.filled)
                  {:on-click #(rf/dispatch [::events/toggle-tool-attribute :sync tool-kw])
                   :aria-label (str "Sync " title " with Metronome")})
       [icon (if synced? :update :update-disabled)]]]
     [modal/config tool-kw]])
  )

(defn toolsbar
  "Contains all the tool-menu components for each individual tool."
  []
  [:div (sx :d--f :jc--c {:style {:flex-wrap "wrap" :gap "1rem"}})
   [tool-menu "Sequencer"  :sequencer]
   [tool-menu "Toggler"    :toggler]
   [tool-menu "Key"        :key]
   [tool-menu "Expression" :expression]
   ])

(defn control-buttons
  "These buttons rest at the bottom of the screen
   and control the metronome and new generation."
  []
  [:div (sx
         :.control-group
         :jc--sb
         :gap--1rem)
   [button ;; Toggle Metronome
    (merge-attrs
     (sx :.filled :.pill :.xlarge :.semi-bold
         {:on-click (fn [_] (rf/dispatch [::events/reset-beat-counter])
                      (metronome/play))})
     (tooltip-attrs {:-text "Start/Stop Metronome"}))
    [icon (if (@metronome/state :isPlaying) :pause :play-arrow)]]
   [button ;; Generate New Data
    (merge-attrs
     (sx :.filled :.pill :.xlarge :.semi-bold
                     {:disabled (@metronome/state :isPlaying)
         ;; pass false on event because generation is manual
                      :on-click (fn [_] (rf/dispatch [::events/generate! false]))})
     (tooltip-attrs {:-text "Generate New Data"}))
    [icon :autorenew]]
   [button ;; Mute Metronome
    (merge-attrs
     (sx :.filled :.pill :.xlarge :.semi-bold
         {:on-click (fn [_] (swap! metronome/state update :silent not))})
     (tooltip-attrs {:-text "Un/mute Metronome"}))
    [icon (if (@metronome/state :silent) :volume-off :volume-up)]]])

(defn metronome-controls
  "Buttons to control the operation of the metronome."
  []
  (let [adjust-tempo (fn [amt _] ;; used with partial; receives event arg
                       (swap! metronome/state update  :tempo #(+ % amt)))]
    (fn []
      [:div (sx :.control-group :pb--0.25rem :gap--0.35rem)
       [button ;; Bump-down Tempo Metronome
        (sx :.metronome-button
            {:on-click (partial adjust-tempo -5)})
        [icon :keyboard-double-arrow-left]]
       [button ;; Nudge-down Tempo Metronome
        (sx :.metronome-button
            {:on-click (partial adjust-tempo -1)})
        [icon :keyboard-arrow-left]]

       [:p
        [:span (sx :.bold) " " (@metronome/state :tempo) " "]
        [:span (sx :.oblique) "BPM"]]

       [button ;; Nudge-up Tempo Metronome
        (sx :.metronome-button
            {:on-click (partial adjust-tempo 1)})
        [icon :keyboard-arrow-right]]
       [button ;; Bump-up Tempo Metronome
        (sx :.metronome-button
            {:on-click (partial adjust-tempo 5)})
        [icon :keyboard-double-arrow-right]]])))

(defn sync-controls
  "Buttons to control the sync of the generation with the metronome."
  []
  (let [bars?  (r/atom false)
        number (r/atom @(rf/subscribe [::subs/beats-to-change]))
        update-cap #(rf/dispatch [::events/set-beat-cap %])
        ;; had to convert this into fn to get most up-to-date value
        total-beats* (fn [] (if @bars? (* 4 @number) @number))]
    (fn []
      [:div
       (sx :.control-group :gap--0.5rem)
       [:p "Change every"]
       [:input (sx :w--3rem :b--1px:solid:black :ta--c
                   {:type :number
                    :style {:align-self :flex-start}
                    :on-change (fn [e] (reset! number (-> e .-target .-value js/parseInt))
                                 (update-cap (total-beats*)))
                    :value @number})]
       [switch
        (sx
         :.xlarge :$switch-width-ratio--2.25
         {:-track-content-on "Bar" :-track-content-off "Beat"
          :-on? @bars?
          :-thumb-attrs (sx :.convex)
          :on-click (fn [_]
                      (swap! bars? not)
                      (update-cap (total-beats*)))})]])))

(defn main-view []
  [:div
   (sx :d--flex
       :w--100%
       :.flex-col-c
       :ai--c)
   [display-panel]
   [toolsbar]
   [:canvas#metrocanvas #_(sx :d--none)]
   [card (sx :.flex-col-c :gap--0.25rem :w--fit-content :pi--2rem :mb--1rem :.rounded)
    [control-buttons]
    [metronome-controls]
    [sync-controls]]
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

(defn ^:dev/before-load stop
  [] ;; reload the page for a fresh start
  (js/location.reload))

;; Dev build fix for strange kushi behavior
(when ^boolean js/goog.DEBUG
  (inject!))