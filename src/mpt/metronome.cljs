(ns mpt.metronome
  (:require [reagent.core :as r]))

;; using whacked's port -- found from -- https://gist.github.com/whacked/e44278a22358574cda3a
;; ref https://github.com/cwilso/metronome/blob/master/js/metronome.js
;; with the metronomeworker.js inlined directly.

;; assumes WebAudio compliance
;; init() expects an existing AudioContext which is initialized and stored in the db;
;; also expects that DOM contains a canvas with id "metrocanvas"

(def state
  (r/atom {
         :audioContext nil
         :isPlaying false         ;; Are we currently playing?
         :startTime nil           ;; The start time of the entire sequence.
         :current16thNote nil     ;; What note is currently last scheduled?
         :tempo 100.0             ;; tempo (in beats per minute)
         :lookahead 25.0          ;; How frequently to call scheduling function (in milliseconds)
         :scheduleAheadTime 0.1   ;; How far ahead to schedule audio (sec)
                                  ;; This is calculated from lookahead, and overlaps
                                  ;; with next interval (in case the timer is late)
         :nextNoteTime 0.0        ;; when the next note is due.
         :noteResolution 0        ;; 0 =16th, 1 =8th, 2 =quarter note
         :noteLength 0.1          ;; length of "beep" (in seconds)
         :canvas nil              ;; the canvas element
         :canvasContext nil       ;; canvasContext is the canvas' context 2D
         :last16thNoteDrawn -1    ;; the last "box" we drew on the screen

         :timerWorker nil         ;; The Web Worker used to fire timer messages
         }
        ))

;; NOTE: this is used in draw();
;; mutable;
(defonce notesInQueue (js/Array)) ;; the notes that have been put into the web audio,
                                  ;; and may or may not have played yet. {note, time}


;; TODO: add this to init()?

;; First, let's shim the requestAnimationFrame API, with a setTimeout fallback
(aset js/window "requestAnimFrame"
      ((fn []
         (or
          (aget js/window "requestAnimationFrame")
          (aget js/window "webkitRequestAnimationFrame")
          (aget js/window "mozRequestAnimationFrame")
          (aget js/window "oRequestAnimationFrame")
          (aget js/window "msRequestAnimationFrame")
          (fn [callback]
            (js/window.setTimeout callback (/ 1000 60)))))))

(defn nextNote []
  ;; Advance current note and time by a 16th note...
  (let [;; Notice this picks up the CURRENT
        ;; tempo value to calculate beat length.
        secondsPerBeat (/ 60.0 (@state :tempo))
        next16thNoteTime (inc (@state :current16thNote))
        ]
    ;; Add beat length to last beat time
    (swap! state assoc
           :nextNoteTime (+ (@state :nextNoteTime) (* 0.25 secondsPerBeat))
           ;; Advance the beat number, wrap to zero
           :current16thNote (if (= 16 next16thNoteTime)
                              (do
                                (js/console.log "new beat")
                                0)
                              next16thNoteTime))))

(defn scheduleNote [beatNumber time]
  ;; push the note on the queue, even if we're not playing.
  (.push notesInQueue #js{:note beatNumber :time time})

  (when-not (or
             ;; playing non-8th 16th notes?
             (and (= 1 (@state :noteResolution))
                  (not= 0 (mod beatNumber 2)))
             ;; playing non-quarter 8th notes?
             (and (= 2 (@state :noteResolution))
                  (not= 0 (mod beatNumber 4)))
             )

    ;; create an oscillator
    (let [context (@state :audioContext)
          osc (.createOscillator context)]
      (doto osc
        (.connect (aget context "destination"))
        (aset "frequency" "value"
              (cond (= 0 (mod beatNumber 16)) 880.0 ;; beat 0 == low pitch
                    (= 0 (mod beatNumber 4))  440.0 ;; quarter notes = medium pitch
                    :else 220.0 ;; other 16th notes = high pitch
                    ))
        (.start time)
        (.stop (+ time (@state :noteLength)))))))

(defn scheduler []
  ;; while there are notes that will need to play before the next interval,
  ;; schedule them and advance the pointer.
  (loop []
    (let [{:keys [nextNoteTime current16thNote scheduleAheadTime audioContext]}
          @state]
     (when (< nextNoteTime (+ audioContext.currentTime scheduleAheadTime))
       (scheduleNote current16thNote nextNoteTime)
       (nextNote)
       (recur)))))

(defn play []
  (swap! state update :isPlaying not)
  (if (@state :isPlaying)
    (do (swap! state assoc
               :current16thNote 0
               :nextNoteTime (aget (@state :audioContext) "currentTime"))
        (.postMessage (@state :timerWorker) "start")
        "stop")
    (do (.postMessage (@state :timerWorker) "stop")
        "play")))

(defn resetCanvas [e]
  (when-let [cnv (@state :canvas)]
    ;; resize the canvas - but remember - this clears the canvas too.
    (doto cnv
      (aset "width" js/window.innerWidth)
      (aset "height" js/window.innerHeight))
    ;; make sure we scroll to the top left.
    (js/window.scrollTo 0 0)))

(defn draw []
  (let [
        currentTime (aget (@state :audioContext) "currentTime")

        currentNote (loop [currentNote (@state :last16thNoteDrawn)]
                      (if (and (< 0 (aget notesInQueue "length"))
                               (< (aget notesInQueue 0 "time") currentTime))
                        (let [next-note (aget notesInQueue 0 "note")]
                          ;; remove note from queue
                          (.splice notesInQueue 0 1)
                          (recur next-note))
                        currentNote))
        ]

    ;; We only need to draw if the note has moved.

    (when (not= currentNote (@state :last16thNoteDrawn))
      (let [cnv (@state :canvas)
            ctx (@state :canvasContext)
            x (js/Math.floor (/ (aget cnv "width") 18))]
        (.clearRect ctx
                    0 0
                    (aget cnv "width") (aget cnv "height"))
        (doseq [i (range 16)]
          (doto ctx
            (aset "fillStyle" (if (= currentNote i)
                                (if (= 0 (mod currentNote 4))
                                  "red"
                                  "blue")
                                "black"))
            (.fillRect (* x (+ 1 i)) 0
                       (/ x 2)
                       x))))
      (swap! state assoc :last16thNoteDrawn currentNote))


    ;; set up to draw again
    (js/window.requestAnimFrame draw)))

;; supposed to be called a la
;; window.addEventListener("load", init );
(defn init [ado-ctx]
  (let [cnv (js/document.getElementById "metrocanvas")
        canv-ctx (.getContext cnv "2d")]
        ;; Included "monkey patch" library original code instructs
    (swap! state assoc
           :audioContext ado-ctx
           :canvas cnv
           :canvasContext canv-ctx)

    (doto canv-ctx
      (aset "strokeStyle" "#fff")
      (aset "lineWidth" 2))

    ;; if we wanted to load audio files, etc., this is where we should do it.


    ;; (doto js/window
    ;;   (aset "onorientationchange" resetCanvas)
    ;;   (aset "onresize" resetCanvas))

    (js/window.requestAnimFrame draw)

    ;; anonymous web worker ref
    ;; http://www.html5rocks.com/en/tutorials/workers/basics/#toc-inlineworkers
    (let [js-fn-text "
var timerID=null;
var interval=100;
self.onmessage=function(e){
	if (e.data=='start') {
		console.log('starting');
		timerID=setInterval(function(){postMessage('tick');},interval)
	}
	else if (e.data.interval) {
		console.log('metronome: Setting timeout interval');
		interval=e.data.interval;
		console.log('interval initialized at '+interval);
		if (timerID) {
			clearInterval(timerID);
			timerID=setInterval(function(){postMessage('tick');},interval)
		}
	}
	else if (e.data=='stop') {
		console.log('stopping');
		clearInterval(timerID);
		timerID=null;
	}
};
postMessage('Metronome initialized.');
"
          blob (js/Blob. #js[js-fn-text]
                         #js{:type "application/javasript"})

          ;; Obtain a blob URL reference to our worker 'file'.
          blobURL (js/URL.createObjectURL blob)

          worker (js/Worker. blobURL)
          ;; to cleanup
          ;; window.URL.revokeObjectURL(blobURL)
          ]

      (aset worker
            "onmessage"
            (fn [e]
              (if (= "tick" (aget e "data"))
                (scheduler)
                (js/console.log (str "metronome: " (aget e "data"))))))
      (.postMessage worker #js{:interval (@state :lookahead)})
      (swap! state assoc :timerWorker worker)))
  )