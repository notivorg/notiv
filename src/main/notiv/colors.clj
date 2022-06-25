(ns notiv.colors
  (:require
   [clojure.java.io :as io]
   [clojure.edn :as edn]))

(def colors (delay (edn/read-string (slurp (io/resource "notiv/colors.edn")))))

(defmacro inline-color [& ks]
  (if-let [color (get-in @colors ks)]
    color
    (throw (ex-info "Color not found" {:ks ks}))))
