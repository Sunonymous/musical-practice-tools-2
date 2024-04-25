(ns mpt.events
  (:require
   [re-frame.core :as rf]
   [tools.sequencer.core :as sqncr]
   [tools.toggler.core   :as toggler]
   [tools.twelve-keys.views :refer [flat-keys]]
   [mpt.db :as db]))

(rf/reg-event-db
 ::initialize-db
 (fn [_] db/default-db))

;; Functions for generating next data are separated from events
;; so that they can be used individually and altogether w/o duplication.

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

;; Now as events.

(rf/reg-event-db ::next-sequence next-sequence)
(rf/reg-event-db ::next-toggle next-toggle)
(rf/reg-event-db ::next-key next-key)

(rf/reg-event-db
 ::generate!
 (fn [{:keys [sync lock] :as db}]
   (let [generate? #(and (sync %) (not (lock %)))]
     (cond-> db
       (generate? :sequencer)
       (next-sequence)

       (generate? :toggler)
       (next-toggle)

       (generate? :key)
       (next-key)))))

;; attr should be a kw matching a set value in db
;; tool is a keyword matching one of the tools
;; simply toggles the presence of a particular tool
(rf/reg-event-db
 ::toggle-tool-attribute
 (fn [db [_ attr tool]]
   (if ((db attr) tool)
     (update db attr disj tool)
     (update db attr conj tool))))