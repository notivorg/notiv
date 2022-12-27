(ns notiv.devcards
  (:require
   [devcards-marked]
   [devcards.core :as devcards]
   [notiv.ui.grid2-cards]))

(set! js/window.DevcardsMarked devcards-marked/marked)

(defn main []
  (devcards/start-devcard-ui!))
