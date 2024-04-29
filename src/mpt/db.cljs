(ns mpt.db
  (:require
   [tools.sequencer.core    :as sqncr]
   [tools.toggler.core      :as toggler]))

(def default-db
  {:audio-context (js/AudioContext.)    ;; JS AudioContext object. Initialized on page load.
   :music {:sequence "1, 2, 3" :key "A" ;; Current iteration of the generations.
           :toggler  0  :expression "Beautifully"}
   :show  #{}                           ;; Set containing the tools shown in display.
   :lock  #{}                           ;; Set containing the tools which should not regenerate.
   :sync  #{}                           ;; Set containing the tools which regenerate on bar.
   :remaining-beats 1                   ;; Dynamic count of beats until next generation.
   :beats-to-change 1                   ;; Alterable cap used to reset remaining-beats.
   :config {:sequencer   sqncr/default-options
            :toggler     toggler/default-values
            :key         #{}            ;; This value is a set containing all keys already used.
            :expression  {:sources #{:articulation :articulationFormal} ;; TODO remove/set proper default
                          :seen    #{}}
           }
  })