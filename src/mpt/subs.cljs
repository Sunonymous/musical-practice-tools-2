(ns mpt.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::audio-context
 (fn [db]
   (:audio-context db)))

(rf/reg-sub ::sequence (fn [db] (get-in db [:music :sequence])))

(rf/reg-sub
 ::toggler
 (fn [db]
   (let [values (get-in db [:config :toggler])
         index  (get-in db [:music  :toggler])]
     (get values index :error!))))

(rf/reg-sub
 ::key (fn [db] (get-in db [:music :key])))

(rf/reg-sub
 ::visible-tools
 (fn [db] (db :show)))

(rf/reg-sub
 ::is-visible?
 (fn [[_ tool]] (rf/subscribe [::visible-tools]))
 (fn [tools [_ tool]] (tools tool)))

(rf/reg-sub ;; for debugging purposes!
 ::full (fn [db] db))