(ns mpt.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::audio-context
 (fn [db]
   (:audio-context db)))