(ns notiv.ui.grid.selection)

(defn make-active-cell-selection [coord]
  {:selection/active-cell coord})

(defn- pin-coord-to-active-cell-row
  "Returns a coord with the part-idx and row-idx of the active cell, and the cell-idx of coord."
  [selection coord]
  (let [[active-cell-part-idx active-cell-row-idx] (:selection/active-cell selection)]
    [active-cell-part-idx active-cell-row-idx (last coord)]))

(defn select-range
  "Returns a range selection including the cells between coord1 and coord2, or between the
  active cell and some coord. Only includes cells which are on the same row as the active cell."
  ([selection coord]
   (select-range selection (:selection/active-cell selection) coord))
  ([selection coord1 coord2]
   (let [coord1 (pin-coord-to-active-cell-row selection coord1)
         coord2 (pin-coord-to-active-cell-row selection coord2)]
     (if (= coord1 coord2)
       ;; Revert to just the active cell if only one cell is in the range
       (make-active-cell-selection coord1)
       (let [[start-cell end-cell] (sort-by last [coord1 coord2])]
         (assoc selection
                :selection/start-cell start-cell
                :selection/end-cell end-cell))))))

(defn range-selection? [selection]
  (and (some? (:selection/start-cell selection))
       (some? (:selection/end-cell selection))))

(defn selected-cells [grid selection]
  (let [[part-idx row-idx] (:selection/active-cell selection)
        [_ _ start-cell-idx] (:selection/start-cell selection)
        [_ _ end-cell-idx] (:selection/end-cell selection)
        row (get-in grid [:grid/parts part-idx :part/rows row-idx])]
    (take (inc (- end-cell-idx start-cell-idx))
          (drop start-cell-idx (:row/cells row)))))
