(ns mpt.db
  (:require
   [clojure.spec.alpha :as s]))

(def default-db
  {:audio-context (js/AudioContext.)})