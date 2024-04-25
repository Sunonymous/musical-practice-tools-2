(ns mpt.events
  (:require
   [re-frame.core :as rf]
   [mpt.db :as db]))

(rf/reg-event-db
 ::initialize-db
 (fn [_] db/default-db))