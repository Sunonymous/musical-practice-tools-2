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

(defonce show-splash-screen? (r/atom true))
(defn splash-screen
  "Screen popping up to distract the user from the css injection."
  []
  (let [_ (js/setTimeout #(reset! show-splash-screen? false) 5000)]
    [:div#splash
     [:h1 (sx :.absolute :.bold :.xxlarge
              {:style {:top :45% :left :10%}})
      "Musical Practice Tools"]
     [:span (sx :.absolute :.large :.oblique
                {:style {:top :50% :left :15%}}) "— Improvise efficiently."]]))

(defn generation-warning
  "A component displaying text about when the next generation
   will occur. Only appears when the metronome is playing
   and at least one tool is synchronized to it."
  []
  (let [remaining-beats @(rf/subscribe [::subs/remaining-beats])
        tools-visible?   (seq @(rf/subscribe [::subs/visible-tools]))
        tools-synced?    (seq @(rf/subscribe [::subs/synced-tools]))]
    [:p (sx :c--white  :.relative :.small :.oblique :ta--right
            {:style {:top "2.5rem" :left "0px"
                     :user-select  :none
                     :transition   "opacity 0.5s"
                     :opacity (if (and (@metronome/state :isPlaying)
                                       tools-synced?
                                       tools-visible?)
                                1 0)}})
     (if (= remaining-beats 1)
       "New generation on next beat."
       (str "New generation in " remaining-beats " beats."))]))

;; may show multiple components until the user toggles a tool
(defonce show-starting-text (r/atom true))

(defn starting-text
  "A short message appearing to guide the user to activate a tool
   to begin."
  []
  [:div (sx :d--f {:style {:opacity (if @show-starting-text 1 0)
                           :position :relative
                           :left :4.5rem :top :5rem}})

   [icon (sx :.xxxlarge :c--gold) :west]
   [:br]
   [:p (sx :.tutorial)
    "Activate a tool to get started."]])

(defn display-panel
  "The main view of the description of the music
   which should be played. Sits at the top of the screen."
  []
  [:div (sx :w--90%
            :.full-rounded
            :mb--1rem
            :p--0.5rem:1rem
            :bgc--white)
   [:div (sx :ta--c :.flex-row-sb :ai--fe)
    [:div (sx :mie--auto :ai--fs :w--fit-content
              {:style {:opacity (if @(rf/subscribe [::subs/is-visible? :key]) 1 0)
                       :transition "opacity 0.25s"}})
     [:p "Key:"]
     [:p (sx :.display-text {:style {:justify-self :flex-start}})
      @(rf/subscribe [::subs/key])]]
    [:div (sx :mis--auto :mbe--auto :w--fit-content
              {:style {:opacity (if @(rf/subscribe [::subs/is-visible? :expression]) 1 0)
                       :transition "opacity 0.25s"}})
     [:p (sx :ta--r) "Expression:"]
     [:p (sx :.large :.bold
             {:style {:justify-self :flex-end}})
      @(rf/subscribe [::subs/expression])]]]
   [:p (sx :ta--c :.xlarge :.bold
           {:style {:white-space :pre
                    :opacity (if @(rf/subscribe [::subs/is-visible? :sequencer]) 1 0)
                    :transition "opacity 0.25s"}})
    @(rf/subscribe [::subs/sequence])]
   [:div (sx :mi--auto :w--fit-content
             {:style {:margin-top :auto
                      :opacity (if @(rf/subscribe [::subs/is-visible? :toggler]) 1 0)
                      :transition "opacity 0.25s"}})
    [:p (sx :.large :.bold) @(rf/subscribe [::subs/toggler])]]])

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
    [:div (sx {:style {:margin-bottom :0.5rem}})
      [:p (sx :c--white :.medium :.extra-bold) title]
     [:div (sx :bgc--white :w--fit-content :p--0.25rem:0.5rem :mb--0.25rem
               {:style {:margin-left :-1rem
                        :padding-left :1rem
                        :border-radius "0 16px 16px 0"}})
      [:div (sx :d--f :pis--0.5rem :pb--0.25rem :gap--8px )
       [button (sx :.toolmenu-button (when visible? :.filled)
                   {:on-click  (fn [_] ;; disable that starting text
                                 (rf/dispatch [::events/toggle-tool-attribute :show tool-kw])
                                 (reset! show-starting-text false))
                    :aria-label (str "Show or Hide " title)})
        [icon (sx :.large) (if visible? :visibility :visibility-off)]]
       (when visible?
         [button (sx :.toolmenu-button (when locked? :.filled)
                     {:on-click (fn [_]
                                  (rf/dispatch [::events/toggle-tool-attribute :lock tool-kw])
                                  (when synced? (rf/dispatch [::events/toggle-tool-attribute :sync tool-kw])))
                      :aria-label (str "Lock " title)})
          [icon (if locked? :lock :lock-open)]])
       (when visible?
         [button (sx :.toolmenu-button
                     {:disabled   locked?
                      :on-click  #(rf/dispatch [(tools->gen-event tool-kw)])
                      :aria-label (str "Generate " title)})
          [icon :autorenew]])
       (when visible?
         [button (sx :.toolmenu-button (when synced? :.filled)
                     {:disabled   locked?
                      :on-click  #(rf/dispatch [::events/toggle-tool-attribute :sync tool-kw])
                      :aria-label (str "Sync " title " with Metronome")})
          [icon (if synced? :update :update-disabled)]])
       (when visible?
         [modal/config tool-kw])]]])
  )

(defn toolsbar
  "Contains all the tool-menu components for each individual tool."
  []
  [:div (sx {:style {:align-self :flex-start}})
   [starting-text]
   [tool-menu "Key"        :key]
   [tool-menu "Expression" :expression]
   [tool-menu "Sequencer"  :sequencer]
   [tool-menu "Toggler"    :toggler]])

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
                   {:type :number :min 1
                    :style {:align-self :flex-start}
                    :on-change (fn [e]
                                 (let [parsed-num (-> e .-target .-value js/parseInt)]
                                   (reset! number (if (js/isNaN parsed-num) 1 parsed-num))
                                   (update-cap (total-beats*))))
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

(defn control-card
  "Stitches together controls for metronome, generation, and sync."
  []
  [card (sx :w--fit-content :.flex-col-c :gap--0.25rem
            :pi--2rem :.rounded
            {:style {:align-self :flex-end}})
   [control-buttons]
   [metronome-controls]
   [sync-controls]])

(defn main-view []
  [:div
   (sx :.flex-col-c :ai--c
       {:style {:overflow :hidden}})
   [display-panel]
   [generation-warning]
   [:canvas#metrocanvas (sx  #_:d--none)]
   [toolsbar]
   [control-card]
   [modal/sunshine-button]
   (when @show-splash-screen?
     [splash-screen]) ;; shown briefly, then deleted
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