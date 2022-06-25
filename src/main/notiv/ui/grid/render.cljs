(ns notiv.ui.grid.render
  (:require
   [notiv.math :as math]))

(defn- render-cell [col-width height x y cell]
  (let [width (* col-width (:cell/col-span cell))]
    (assoc cell :cell/rect (math/rect x y width height))))

(defn- render-row [row x y height col-width]
  (let [next-x (volatile! x)
        rendered-cells (mapv
                        (fn [cell]
                          (let [rendered-cell (render-cell col-width height @next-x y cell)
                                [cell-width _] (math/rect-size (:cell/rect rendered-cell))]
                            (vswap! next-x + cell-width)
                            rendered-cell))
                        (:row/cells row))
        [bottom-right-x _] (math/rect-bottom-right (:cell/rect (last rendered-cells)))]
    (assoc row
           :row/cells rendered-cells
           :row/rect (math/rect x y (- bottom-right-x x) height))))

(defn- grid-col-width [grid]
  (/ (:grid/base-cell-size grid) (:grid/base-cell-col-span grid)))

(defn- render-part [grid part part-idx parts-count]
  (let [col-width (grid-col-width grid)
        next-y (volatile! (* part-idx (:grid/base-cell-size grid)))
        rendered-rows (mapv
                       (fn [row]
                         (let [rendered-row (render-row row 0 @next-y (:grid/base-cell-size grid) col-width)]
                           (vswap! next-y + (* parts-count (:grid/base-cell-size grid)))
                           rendered-row))
                       (:part/rows part))]
    (assoc part :part/rows rendered-rows)))

(defn render-grid
  "Returns a rendered grid.

  The rendered grid is the grid data augmented with rects describing the size and position
  of its elements."
  [grid]
  (let [rendered-parts (vec (map-indexed
                             (fn [part-idx part]
                               (render-part grid part part-idx (count (:grid/parts grid))))
                             (:grid/parts grid)))
        [bottom-right-x bottom-right-y] (math/rect-bottom-right (:row/rect (last (:part/rows (last rendered-parts)))))]
    (assoc grid
           :grid/parts rendered-parts
           :grid/rect (math/rect 0 0 bottom-right-x bottom-right-y))))
