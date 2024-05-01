(ns mpt.subs
  (:require
   [re-frame.core :as rf]
   [mpt.util :as util]))

(rf/reg-sub
 ::audio-context
 (fn [db]
   (:audio-context db)))

(rf/reg-sub ::sequence (fn [db] (get-in db [:music :sequence])))

(rf/reg-sub
 ::sequencer-options
 (fn [db]
   (get-in db [:config :sequencer])))

(rf/reg-sub
 ::max-length-possible
 (fn [] (rf/subscribe [::sequencer-options]))
 (fn [options] (util/max-possible-length options)))

(rf/reg-sub
 ::toggler
 (fn [db]
   (let [values (get-in db [:config :toggler])
         index  (get-in db [:music  :toggler])]
     (get values index :error!))))

(rf/reg-sub
 ::toggler-values
 (fn [db] (get-in db [:config :toggler])))

(rf/reg-sub
 ::key (fn [db] (get-in db [:music :key])))

(rf/reg-sub
 ::excluded-keys (fn [db] (get-in db [:config :key :excluded])))

(rf/reg-sub
 ::seen-keys (fn [db] (get-in db [:config :key :seen])))

(rf/reg-sub
 ::expression (fn [db] (get-in db [:music :expression])))

(rf/reg-sub
 ::remaining-beats
 (fn [db] (db :remaining-beats)))

(rf/reg-sub
 ::beats-to-change
 (fn [db] (db :beats-to-change)))

(rf/reg-sub
 ::visible-tools
 (fn [db] (db :show)))

(rf/reg-sub
 ::locked-tools
 (fn [db] (db :lock)))

(rf/reg-sub
 ::synced-tools
 (fn [db] (db :sync)))

(rf/reg-sub
 ::is-visible?
 (fn [[_ tool]] (rf/subscribe [::visible-tools]))
 (fn [tools [_ tool]] (tools tool)))

(rf/reg-sub
 ::is-locked?
 (fn [[_ tool]] (rf/subscribe [::locked-tools]))
 (fn [tools [_ tool]] (tools tool)))

(rf/reg-sub
 ::is-synced?
 (fn [[_ tool]] (rf/subscribe [::synced-tools]))
 (fn [tools [_ tool]] (tools tool)))

(rf/reg-sub
 ::config-boolean
 (fn [db [_ config-kw]] (get-in db [:config config-kw])))

(rf/reg-sub     ;; honestly, strange naming here. it's because I inadvertently
 ::nested-value ;; spread out where configuration is placed within db
 (fn [db [_ db-path]] (get-in db db-path :NOT-FOUND)))

(rf/reg-sub ;; for debugging purposes!
 ::full (fn [db] db))