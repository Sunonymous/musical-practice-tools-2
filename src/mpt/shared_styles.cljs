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

(defclass control-group
  :w--100%
  :d--f
  :jc--c
  :bgc--white
  :ai--c)

(defclass toolmenu-button
  :bgc--$gray-350 :.full-rounded :.medium :.semi-bold :.neutral :b--1px:solid:transparent
  :hover:bgc--$gray-750 :hover:c--white)

(defclass metronome-button
  :.filled :.pill :.small :.semi-bold :bgc--gray)

(defclass display-text
  :.xxlarge :.bold)