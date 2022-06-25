(ns notiv.util
  (:require
   [clojure.core.async :as async]
   [clojure.string :as string]
   [clojure.java.io :as io]))

(defn print-prefixed-lines [prefix stream]
  (with-open [reader (io/reader stream)]
    (binding [*in* reader]
      (loop []
        (when-let [line (read-line)]
          (when-not (string/blank? line)
            (println prefix line))
          (recur))))))

(defn print-prefixed-process-output [prefix process]
  (async/thread (print-prefixed-lines prefix (:out process)))
  (print-prefixed-lines prefix (:err process)))
