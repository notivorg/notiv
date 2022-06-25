(ns notiv.colors
  (:require
   [goog.string :as gstring]
   [goog.string.format]
   [goog.color :as gcolor])
  (:require-macros
   [notiv.colors :refer [inline-color]]))

(def gray-600 (inline-color "gray" "600"))

(def gray-700 (inline-color "gray" "700"))

(def blue-500 (inline-color "blue" "500"))

(def red-500 (inline-color "red" "500"))

(defn transparent [hex alpha]
  (let [[r g b] (gcolor/hexToRgb hex)]
    (gstring/format "rgba(%s, %s, %s, %s)" r g b alpha)))
