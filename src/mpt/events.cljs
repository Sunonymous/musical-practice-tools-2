(ns mpt.events
  (:require
   [re-frame.core :as rf]
   [tools.sequencer.core :as sqncr]
   [tools.toggler.core   :as toggler]
   [mpt.db :as db]))

(rf/reg-event-db
 ::initialize-db
 (fn [_] db/default-db))

(rf/reg-event-db
 ::generate-sequence
 (fn [db]
   (assoc-in db [:music :sequence]
             (sqncr/generate! (get-in db [:config :sequencer])))))

(rf/reg-event-db
 ::toggle-value
 (fn [db]
   (assoc-in db [:music :sequence]
             (toggler/next-index
              (get-in db [:config :toggler  :values])
              (get-in db [:config :toggler :index])))))

(rf/reg-event-db
 ::generate!
 (fn [db]
   (let [to-sync (db :sync)
         config  (db :config)]
     (cond-> db
       (to-sync :sequencer)
       (assoc-in [:music :sequence]
                 (sqncr/generate! (config :sequencer)))

       (to-sync :toggler)
       (assoc-in [:music :toggler]
                 (toggler/next-index
                  (config :toggler)
                  (get-in db [:music :toggler])))

       (to-sync :key)
       (assoc-in [:music :key] 42)
       ))))

(rf/reg-event-db
 ::toggle-visible
 (fn [db [_ tool]]
   (if ((db :show) tool)
     (update db :show disj tool)
     (update db :show conj tool))))