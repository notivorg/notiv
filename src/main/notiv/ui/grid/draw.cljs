(ns notiv.ui.grid.draw
  (:require
   [notiv.colors :as colors]
   [notiv.math :as math]
   [notiv.canvas :as canvas]
   [notiv.ui.grid.selection :as grid-selection]))

(defn draw-grid! [ctx rendered-grid]
  (canvas/with-restore ctx
    (doseq [part (:grid/parts rendered-grid)
            row (:part/rows part)
            cell (:row/cells row)
            :let [[x y] (math/rect-top-left (:cell/rect cell))
                  [width height] (math/rect-size (:cell/rect cell))]]
      (set! (.-fillStyle ctx) colors/gray-700)
      (.fillRect ctx x y width height)

      (set! (.-lineWidth ctx) 1)
      (set! (.-strokeStyle ctx) colors/gray-600)
      (.strokeRect ctx (+ x 0.5) (+ y 0.5) width height)

      (when (:cell/parse-error cell)
        (set! (.-fillStyle ctx) (colors/transparent colors/red-500 0.15))
        (.fillRect ctx x y width height))

      (when (:cell/value cell)
        (let [[center-x center-y] (math/rect-center (:cell/rect cell))]
          (set! (.-fillStyle ctx) "white")
          (set! (.-font ctx) "16px 'Open Sans'")
          (set! (.-textAlign ctx) "center")
          (set! (.-textBaseline ctx) "bottom")
          (.fillText ctx (:cell/value cell) (inc center-x) (+ 2 (/ 16 2) center-y) width))))))

(defn- draw-selection-active-cell! [ctx rendered-grid selection]
  (canvas/with-restore ctx
    (let [[part-idx row-idx cell-idx] (:selection/active-cell selection)
          cell (get-in rendered-grid [:grid/parts part-idx :part/rows row-idx :row/cells cell-idx])
          [x y] (math/rect-top-left (:cell/rect cell))
          [width height] (math/rect-size (:cell/rect cell))]
      (set! (.-lineWidth ctx) 2)
      (set! (.-strokeStyle ctx) colors/blue-500)
      (.strokeRect ctx (+ x 1.5) (+ y 1.5) (- width 2) (- height 2)))))

(def ^:private selection-range-fill-style
  (colors/transparent colors/blue-500 0.15))

(defn- range-selection->rect [rendered-grid selection]
  (when (grid-selection/range-selection? selection)
    (let [[part-idx row-idx start-cell-idx] (:selection/start-cell selection)
          [_ _ end-cell-idx] (:selection/end-cell selection)
          row (get-in rendered-grid [:grid/parts part-idx :part/rows row-idx])
          start-cell (nth (:row/cells row) start-cell-idx)
          end-cell (nth (:row/cells row) end-cell-idx)]
      (math/bounding-rect (:cell/rect start-cell) (:cell/rect end-cell)))))

(defn draw-selection! [ctx rendered-grid selection]
  (when selection
    (canvas/with-restore ctx
      ;; Only draw the range when the selection contains a range
      (when-let [selection-rect (range-selection->rect rendered-grid selection)]
        (set! (.-fillStyle ctx) selection-range-fill-style)
        (.fillRect ctx
                   (inc (.-left selection-rect))
                   (inc (.-top selection-rect))
                   (dec (.-width selection-rect))
                   (dec (.-height selection-rect)))

        (set! (.-lineWidth ctx) 1)
        (set! (.-strokeStyle ctx) colors/blue-500)
        (.strokeRect ctx
                     (+ (.-left selection-rect) 0.5)
                     (+ (.-top selection-rect) 0.5)
                     (.-width selection-rect)
                     (.-height selection-rect)))

      ;; Always draw the active cell above the range
      (draw-selection-active-cell! ctx rendered-grid selection))))

(defn draw-pending-selection! [ctx rendered-grid selection]
  (when selection
    (canvas/with-restore ctx
      (when-let [selection-rect (range-selection->rect rendered-grid selection)]
        (set! (.-fillStyle ctx) selection-range-fill-style)
        (.fillRect ctx
                   (inc (.-left selection-rect))
                   (inc (.-top selection-rect))
                   (dec (.-width selection-rect))
                   (dec (.-height selection-rect))))
      (draw-selection-active-cell! ctx rendered-grid selection))))
