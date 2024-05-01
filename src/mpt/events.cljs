(ns mpt.events
  (:require
   [tools.expression.core   :refer [from-all]  ]
   [clojure.string          :refer [split trim]]
   [tools.sequencer.core    :as sqncr]
   [mpt.util                :as util]
   [re-frame.core           :as rf]
   [mpt.db                  :as db]))

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
              (util/next-index
               (get-in db [:config :toggler])
               (get-in db [:music :toggler])))))

(defn next-key
  [db]
  (when-not ((db :lock) :key)
    (let [old-keys       (get-in db [:config :key :seen])
          excluded-keys  (get-in db [:config :key :excluded])
          full-keys      (remove excluded-keys (if (db :config :use-sharps?)
                                                 db/musical-keys-with-sharps
                                                 db/musical-keys-with-flats))
          potential-keys (remove old-keys full-keys)]
      (when-not (empty? full-keys) ;; prevent issue if all keys are excluded
        (if (empty? potential-keys)
          (let [next-key (rand-nth full-keys)]
            (-> db
                (assoc-in [:music  :key] next-key)
                (assoc-in [:config :key :seen] #{next-key})))
          (let [next-key (rand-nth potential-keys)]
            (-> db
                (assoc-in  [:music  :key] next-key)
                (update-in [:config :key :seen] conj next-key))))))))

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

;; Configuration Events

(rf/reg-event-db
 ::toggle-boolean-config
 (fn [db [_ config-kw]]
   (update-in db [:config config-kw] not)))

(rf/reg-event-db
 ::set-numeric-config
 (fn [db [_ db-path value]]
   (let [parsed-num (if (number? value) value (js/parseInt value))]
     (assoc-in db db-path parsed-num))))

;; these events are very similar... numeric will cast to number if needed

(rf/reg-event-db
 ::set-string-config
 (fn [db [_ db-path str]]
   (assoc-in db db-path str)))

(rf/reg-event-db
 ;; this event prevents the length of the sequence from exceeding the max possible
 ::enforce-max-length
 (fn [db]
   (let [options (get-in db [:config :sequencer])
         len     (options :len)
         cap     (util/max-possible-length (get-in db [:config :sequencer]))]
     (if (< cap len)
       (assoc-in db [:config :sequencer :len] cap)
       db))))

(rf/reg-event-db
 ::set-expression-source
 (fn [db [_ source]]
   (let [formatted-source (keyword source)]
     (-> db
         (assoc-in [:config :expression :sources] #{formatted-source})
         (assoc-in [:config :expression :seen]    #{}))))) ;; reset "seen" values

(rf/reg-event-db
 ::set-toggler-values
 (fn [db [_ values-str]]
   (when (seq (trim values-str)) ;; only work with non-empty string
     (let [formatted-values (map trim (split values-str #","))]
       (js/console.log "Changed toggler values.")
       (-> db
           (assoc-in [:config :toggler] (vec formatted-values))
           (assoc-in [:music  :toggler] 0)))))) ;; reset active index

(rf/reg-event-db
 ::toggle-exclude-key
 (fn [db [_ key]]
   (when (util/is-musical-key? key)
     (let [is-excluded? ((get-in db [:config :key :excluded]) key)]
       (update-in db [:config :key :excluded] (if is-excluded? disj conj) key)))))

(rf/reg-event-db
 ::toggle-seen-key
 (fn [db [_ key]]
   (when (util/is-musical-key? key)
     (let [is-seen? ((get-in db [:config :key :seen]) key)]
       (update-in db [:config :key :seen] (if is-seen? disj conj) key)))))

(rf/reg-event-db
 ::reset-keys
 (fn [db]
   (-> db
       (assoc-in [:config :key :excluded] #{})    ;; should we reset this?
       (assoc-in [:config :key :seen]     #{})))) ;; should we include generated key?

(rf/reg-event-db
 ::focus-key ;; sets key to active generation and sees it
 (fn [db [_ key]]
   (when (util/is-musical-key? key)
     (-> db
         (assoc-in [:music :key] key)
         (update-in [:config :key :seen] conj key)))))