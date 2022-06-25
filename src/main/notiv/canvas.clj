(ns notiv.canvas)

(defmacro with-restore [ctx & body]
  `(do
     (.save ~ctx)
     (try
       ~@body
       (finally
         (.restore ~ctx)))))
