(ns notiv.ui.grid.context-menu
  (:require
   ["react-dom" :as ReactDOM]
   [helix.core :as helix :refer [defnc $]]
   [helix.hooks :as hooks]
   [helix.dom :as dom]
   [notiv.ui.context-menu :as context-menu]
   [notiv.ui.grid.selection :as grid-selection]
   [notiv.math :as math]))

(defnc ^:private SubdivideCountButton [{:keys [children] :as props}]
  (dom/button
   {:class ["px-2" "bg-gray-800" "hover:bg-blue-500" "rounded-full" "leading-tight"]
    & props}
   children))

(defnc ^:private SubdivideAction [{:keys [on-action]}]
  (dom/div
   {:class ["px-4" "py-1" "text-gray-200" "text-sm"]}
   (dom/div "Subdivide into...")
   (dom/div
    {:class ["flex" "mt-1" "space-x-1"]}
    ($ SubdivideCountButton {:on-click #(on-action 2)} "2")
    ($ SubdivideCountButton {:on-click #(on-action 3)} "3")
    ($ SubdivideCountButton {:on-click #(on-action 4)} "4")
    ($ SubdivideCountButton {:on-click #(on-action 5)} "5")
    ($ SubdivideCountButton {:on-click #(on-action 6)} "6")
    ($ SubdivideCountButton {:on-click #(on-action 7)} "7"))))

(defnc ^:private AddRowAction [{:keys [on-action]}]
  (dom/button
   {:class ["w-full"
            "px-4"
            "py-1"
            "text-sm"
            "text-left"
            "text-gray-200"
            "hover:bg-blue-500"]
    :on-click #(on-action)}
   "Add row at end"))

(defn- map-cells [f grid]
  (update grid
          :grid/parts
          (fn [parts]
            (mapv (fn [part]
                    (update part
                            :part/rows
                            (fn [rows]
                              (mapv (fn [row]
                                      (update row
                                              :row/cells
                                              (fn [cells]
                                                (mapv f cells))))
                                    rows))))
                  parts))))

(defn- subdivide-grid-selection [grid selection target-division]
  (let [total-cols (reduce + (map :cell/col-span (get-in grid [:grid/parts 0 :part/rows 0 :row/cells])))
        selected-cells (grid-selection/selected-cells grid selection)
        selected-cols (reduce + (map :cell/col-span selected-cells))
        next-total-cols (math/lcm (* (/ target-division selected-cols) total-cols) total-cols)
        cols-scale-factor (/ next-total-cols total-cols)
        [part-idx row-idx] (:selection/active-cell selection)
        [_ _ start-cell-idx] (:selection/start-cell selection)
        [_ _ end-cell-idx] (:selection/end-cell selection)
        next-grid (map-cells (fn [cell] (update cell :cell/col-span * cols-scale-factor)) grid)
        next-grid (update next-grid :grid/base-cell-col-span * cols-scale-factor)
        next-grid (update-in next-grid
                             [:grid/parts part-idx :part/rows row-idx :row/cells]
                             (fn [cells]
                               (vec (concat (take start-cell-idx cells)
                                            (repeat target-division {:cell/col-span (/ (* selected-cols cols-scale-factor) target-division)})
                                            (drop (inc end-cell-idx) cells)))))]
    next-grid))

(defn- append-row [grid]
  (let [total-cols (reduce + (map :cell/col-span (get-in grid [:grid/parts 0 :part/rows 0 :row/cells])))
        cells (vec (repeat (/ total-cols (:grid/base-cell-col-span grid)) {:cell/col-span (:grid/base-cell-col-span grid)}))]
    (update grid
            :grid/parts
            (fn [parts]
              (mapv (fn [part]
                      (update part
                              :part/rows
                              conj
                              {:row/cells cells}))
                    parts)))))

(defnc ContextMenu [{:keys [position on-close grid on-grid-change selection on-selection-change]}]
  (let [ref (hooks/use-ref nil)
        context-menu-portal-ref (hooks/use-context context-menu/context-menu-portal-context)]
    (hooks/use-effect
     [on-close]
     (let [f (fn [^js event]
               (when-not (or (= (.-current ref) (.-target event))
                             (.contains (.-current ref) (.-target event)))
                 (on-close)))]
       (.addEventListener js/window "mousedown" f)
       (fn []
         (.removeEventListener js/window "mousedown" f))))
    (ReactDOM/createPortal
     (dom/div
      {:ref ref
       :style {:top (second position) :left (first position)}
       :class ["absolute" "py-2" "rounded" "bg-gray-900" "shadow"]}
      ($ SubdivideAction
         {:on-action
          (fn [target-division]
            (on-grid-change (subdivide-grid-selection grid selection target-division))
            (on-selection-change nil)
            (on-close))})
      (dom/div
       {:class ["mt-2"]}
       ($ AddRowAction
          {:on-action
           (fn []
             (on-grid-change (append-row grid))
             (on-close))})))
     @context-menu-portal-ref)))
