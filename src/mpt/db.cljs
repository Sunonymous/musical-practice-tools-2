(ns mpt.db)

(def musical-keys-with-flats  ["A" "B♭" "B" "C" "D♭" "D" "E♭" "E" "F" "G♭" "G" "A♭"])
(def musical-keys-with-sharps ["A" "A♯" "B" "C" "C♯" "D" "D♯" "E" "F" "F♯" "G" "G♯" ])

(def default-sequencer-options
  {:delimiter "  ->  " ;; string to separate number
   :min       1        ;; lowest possible value
   :max       8        ;; highest possible value
   :len       4        ;; length of sequence
   :sort      nil      ;; sort may be < or >     ;; TODO implement sorting
   :dups      1})      ;; dups key is the limit of duplicate nums in the sequence.
                       ;; dups of 1 means each number is unique, 2 means that each number
                       ;;   may appear twice, and so on

(defonce default-toggler-values ["Ascending" "Descending"])

(def default-db
  {:audio-context (js/AudioContext.)    ;; JS AudioContext object. Initialized on page load.
   :music {:sequence "1, 2, 3" :key "A" ;; Current iteration of the generations.
           :toggler  0  :expression "Beautifully"}
   :show  #{}                           ;; Set containing the tools shown in display.
   :lock  #{}                           ;; Set containing the tools which should not regenerate.
   :sync  #{}                           ;; Set containing the tools which regenerate on bar.
   :remaining-beats 1                   ;; Dynamic count of beats until next generation.
   :beats-to-change 1                   ;; Alterable cap used to reset remaining-beats.
   :config {:sequencer   default-sequencer-options
            :toggler     default-toggler-values
            :key         {:excluded #{}
                          :seen     #{}} ;; This value is a set containing all keys already used.
            :expression  {:sources #{:mood}
                          :seen    #{}}
            :use-sharps?         false   ;; Use sharps instead of flats for keys
            :alert-on-complete?  true    ;; Pops an alert when user completes all keys
            :use-roman-numerals? false   ;; use roman numerals to display sequences
            :vary-roman-case?    false   ;; vary the case of the roman numerals
            }
  })