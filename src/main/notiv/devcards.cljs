(ns notiv.devcards
  (:require
   [devcards.core :as devcards]
   [devcards-marked]))

(set! js/window.DevcardsMarked devcards-marked/marked)

(defn main []
  (devcards/start-devcard-ui!))
