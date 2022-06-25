(ns notiv.import-tailwind-colors
  (:require
   [clojure.java.io :as io]
   [clojure.pprint :as pprint]
   [babashka.process :as process]
   [cheshire.core :as json]))

(defn- get-tailwind-colors []
  (->> (process/process ["node" "--eval" "console.log(JSON.stringify(require('tailwindcss/colors')))"])
       :out
       (slurp)
       (json/parse-string)))

(defn import-tailwind-colors [output-path]
  (println (str "Importing tailwindcss/colors to " output-path))
  (spit
   (io/file output-path)
   (with-out-str
     (println ";; Imported from tailwindcss/colors")
     (pprint/pprint (get-tailwind-colors))))
  (println "Done."))
