(ns notiv.util)

(defn offset-client-coord [el x y]
  (let [rect (.getBoundingClientRect el)]
    [(- x (.-left rect)) (- y (.-top rect))]))
