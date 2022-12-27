(ns notiv.ui.grid2-cards
  (:require
   [devcards.core :as devcards :refer [defcard]]))

(defonce example-state* (atom {}))

(defcard example-state example-state*)

(defcard example-grid
  (fn [state]
    )
  example-state*)
