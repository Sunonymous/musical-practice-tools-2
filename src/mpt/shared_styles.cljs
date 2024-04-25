(ns mpt.shared-styles
  (:require
   [kushi.core :refer (defclass)]))

(defclass full-rounded
  :beer--8px
  :besr--8px
  :bssr--8px
  :bser--8px)

(defclass full-slight-rounded
  :beer--4px
  :besr--4px
  :bssr--4px
  :bser--4px)

(defclass toolmenu-button
  :.pill :.small :.semi-bold)

(defclass display-text
  :.xxlarge :.bold :.black)