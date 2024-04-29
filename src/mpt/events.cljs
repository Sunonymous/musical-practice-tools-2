(ns mpt.events
  (:require
   [re-frame.core :as rf]
   [tools.sequencer.core :as sqncr]
   [tools.toggler.core   :as toggler]
   [tools.twelve-keys.views :refer [flat-keys]]
   [tools.expression.core :refer [from-all]]
   [mpt.db :as db]))

(rf/reg-event-db
 ::initialize-db
 (fn [_] db/default-db))

;; Functions for generating next data are separated from events
;; so that they can be used individually and altogether w/o duplication.

;; First as functions.

(defn next-sequence
  [db]
  (when-not ((db :lock) :sequencer)
    (assoc-in db [:music :sequence]
              (sqncr/generate! (get-in db [:config :sequencer])))))

(defn next-toggle
  [db]
  (when-not ((db :lock) :toggler)
    (assoc-in db [:music :toggler]
              (toggler/next-index
               (get-in db [:config :toggler])
               (get-in db [:music :toggler])))))

(defn next-key
  [db]
  (when-not ((db :lock) :key)
    (let [old-keys       (get-in db [:config :key])
          potential-keys (remove old-keys flat-keys)]
      (if (empty? potential-keys)
        (let [next-key (rand-nth flat-keys)]
          (-> db
              (assoc-in [:music  :key] next-key)
              (assoc-in [:config :key] #{next-key})))
        (let [next-key (rand-nth potential-keys)]
          (-> db
              (assoc-in  [:music  :key] next-key)
              (update-in [:config :key] conj next-key)))))))

;; TODO generalize this into a pattern that could be used between these functions

(defn next-expression
  [db]
  (when-not ((db :lock) :expression)
    (let [old-expr       (get-in db [:config :expression :seen])
          full-expr      (from-all (get-in db [:config :expression :sources]))
          potential-expr (remove old-expr full-expr)]
      (if (empty? potential-expr)
        (let [next-expr (rand-nth full-expr)]
          (-> db
              (assoc-in [:music  :expression] next-expr)
              (assoc-in [:config :expression :seen] #{next-expr})))
        (let [next-expr (rand-nth potential-expr)]
          (-> db
              (assoc-in  [:music  :expression] next-expr)
              (update-in [:config :expression :seen] conj next-expr)))))))

;; Now as events.

(rf/reg-event-db ::next-sequence next-sequence)
(rf/reg-event-db ::next-toggle next-toggle)
(rf/reg-event-db ::next-key next-key)
(rf/reg-event-db ::next-expression next-expression)

(rf/reg-event-db
 ::generate!
 (fn [{:keys [sync lock] :as db} [_ on-sync?]]
   (let [generate? #(and (if on-sync?
                           (sync %)
                           true)
                         (not (lock %)))]
     (cond-> db
       (generate? :sequencer)
       (next-sequence)

       (generate? :toggler)
       (next-toggle)

       (generate? :key)
       (next-key)

       (generate? :expression)
       (next-expression)))))

;; attr should be a kw matching a set value in db
;; tool is a keyword matching one of the tools
;; simply toggles the presence of a particular tool
(rf/reg-event-db
 ::toggle-tool-attribute
 (fn [db [_ attr tool]]
   (if ((db attr) tool)
     (update db attr disj tool)
     (update db attr conj tool))))

(rf/reg-event-db
 ::set-beat-cap
 (fn [db [_ beats]]
   (js/console.log "set-beat-cap to: " beats)
   (assoc db :beats-to-change beats)))

(rf/reg-event-db
 ::reset-beat-counter
 (fn [db]
   (assoc db :remaining-beats (get db :beats-to-change))))

;; :remaining-beats counts down until 0, at which point it is reset
(rf/reg-event-db
 ::next-beat
 (fn [db]
   (let [remaining-beats (db :remaining-beats)]
     (if (= remaining-beats 1)
       (do
         (rf/dispatch [::generate! true]) ;; pass true to indicate sync source
         (rf/dispatch [::reset-beat-counter]))
       (update db :remaining-beats dec)))))