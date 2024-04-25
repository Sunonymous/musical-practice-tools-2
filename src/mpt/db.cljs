(ns mpt.db
  (:require
   [tools.sequencer.core    :as sqncr]
   [tools.toggler.core      :as toggler]
   [tools.twelve-keys.views :as twelve-keys]
   [clojure.spec.alpha   :as s]))

(def default-db
  {:audio-context (js/AudioContext.)    ;; JS AudioContext object. Initialized on page load.
   :music {:sequence "1, 2, 3" :key "A" ;; Current iteration of the generations.
           :toggler  0}
   :show  #{}                           ;; Set containing the tools shown in display.
   :sync  #{:sequencer :toggler}        ;; Set containing the tools which regenerate on bar.
   :config {:sequencer   sqncr/default-options
            :toggler     toggler/default-values
            :twelve-keys twelve-keys/keys-with-flats}
  })