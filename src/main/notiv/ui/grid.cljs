(ns notiv.ui.grid
  (:require
   [lambdaisland.glogi :as log]
   [helix.core :refer [defnc $]]
   [helix.hooks :as hooks]
   [helix.dom :as dom]
   [notiv.util :as util]
   [notiv.math :as math]
   [notiv.canvas :as canvas]
   [notiv.ui.grid.render :as grid-render]
   [notiv.ui.grid.parse :as grid-parse]
   [notiv.ui.grid.draw :as grid-draw]
   [notiv.ui.grid.selection :as grid-selection]
   [notiv.ui.grid.context-menu :as grid-context-menu]
   [clojure.string :as string]))

;; Displaying the grid is broken down into two steps:
;;
;; 1. Rendering
;;    The grid data and sizes are rendered into a data structure describing the
;;    coordinates of the cells. This is called the "rendered grid" or just "grid".
;;    This data structure is used in the following step, and is also used for
;;    looking up the cell/row based on the offset mouse coordinates within mouse
;;    events, i.e. to determine which cell was clicked on.
;;
;; 2. Drawing
;;    The rendered grid is drawn to a canvas element. This is done as opposed to
;;    using a CSS layout for the sake of simplicity and performance, and to ensure
;;    accurate rendering. Accessibility is a concern but is probably easier to
;;    implement as a separate layer anyway.

(defn- use-rendered-grid [grid]
  (hooks/use-memo
   [grid]
   (let [rendered-grid (->> grid (grid-render/render-grid) (grid-parse/parse-grid))]
     (log/trace ::grid-changed {:grid grid :rendered-grid rendered-grid})
     rendered-grid)))

(def ^:private loading-font
  (.. js/document -fonts (load "16px 'Open Sans'")))

(defn- draw! [canvas rendered-grid selection pending-selection]
  (-> loading-font
      (.then
       (fn []
         (let [ctx (.getContext canvas "2d" #js {:alpha false})]
           (canvas/with-restore ctx
             (.scale ctx (.-devicePixelRatio js/window) (.-devicePixelRatio js/window))
             (.clearRect ctx 0 0 (.-width canvas) (.-height canvas))
             (grid-draw/draw-grid! ctx rendered-grid)
             (cond
               pending-selection (grid-draw/draw-pending-selection! ctx rendered-grid pending-selection)
               selection (grid-draw/draw-selection! ctx rendered-grid selection))))))))

(defn- all-grid-rows-ordered
  "Returns a seq of all rows in the grid in visual order as [part-idx row-idx row] vectors."
  [grid]
  (apply mapcat vector
         (map-indexed (fn [part-idx part]
                        (map-indexed (fn [row-idx row] [part-idx row-idx row])
                                     (:part/rows part)))
                      (:grid/parts grid))))

(defn- canvas-coord->cell-coord
  "Takes a coordinate vector relative to the grid canvas and returns a [part-idx row-idx cell-idx]
  vector for the cell containing it. Coordinates outside of the grid will resolve to the
  nearest cell."
  [rendered-grid canvas-coord]
  (let [[x y] canvas-coord
        vertical-distance (math/rect-vertical-distance (:grid/rect rendered-grid) y)
        [part-idx row-idx row]
        (cond
          ;; If the coord is above the grid, take the first part, first row
          (neg? vertical-distance)
          [0 0 (first (:part/rows (first (:grid/parts rendered-grid))))]
          ;; If the coord is below the grid, take the last part, last row
          (pos? vertical-distance)
          (let [last-part-idx (dec (count (:grid/parts rendered-grid)))]
            [last-part-idx
             (dec (count (:part/rows (last (:grid/parts rendered-grid)))))
             (last (:part/rows (last (:grid/parts rendered-grid))))])
          :else
          (let [rows (all-grid-rows-ordered rendered-grid)]
            (loop [i 0]
              (let [[part-idx row-idx row] (nth rows i)]
                (if (math/rect-contains-point-horizontally? (:row/rect row) y)
                  [part-idx row-idx row]
                  (recur (inc i)))))))
        horizontal-distance (math/rect-horizontal-distance (:grid/rect rendered-grid) x)
        cell-idx
        (cond
          (neg? horizontal-distance)
          0
          (pos? horizontal-distance)
          (dec (count (:row/cells row)))
          :else
          (loop [cell-idx 0]
            (let [cell (nth (:row/cells row) cell-idx)]
              (if (math/rect-contains-point-vertically? (:cell/rect cell) x)
                cell-idx
                (recur (inc cell-idx))))))]
    [part-idx row-idx cell-idx]))

(defnc ^:private EditingCell [{:keys [cell value on-change]}]
  (let [[left top] (math/rect-top-left (:cell/rect cell))
        [width height] (math/rect-size (:cell/rect cell))]
    (dom/div
     {:class ["absolute" "p-[3px]"]
      :style {:left left :top top :width width :height height}}
     (dom/input
      {:class ["w-full" "h-full" "bg-gray-700" "text-center" "text-white" "focus:outline-none"]
       :type "text"
       :auto-focus true
       :value (or value "")
       :on-change #(on-change (.. ^js % -target -value))}))))

(defnc ^:private PartTitlesColumn [{:keys [rendered-grid]}]
  (dom/div
   (for [[idx row part] (map vector
                             (range)
                             (mapcat :part/rows (:grid/parts rendered-grid))
                             (cycle (:grid/parts rendered-grid)))
         :let [[_ row-height] (math/rect-size (:row/rect row))]]
     (dom/div
      {:key idx
       :style {:height row-height}
       :class ["flex"
               "items-center"
               "px-4"
               "text-gray-400"
               "text-sm"]}
      (:part/title part)))))

(defnc ^:private RowLabelsColumn [{:keys [rendered-grid]}]
  (let [indexed-rows (map vector
                          (range)
                          (mapcat :part/rows (:grid/parts rendered-grid)))]
    (dom/div
     (for [[row-number rows] (map vector
                                  (drop 1 (range))
                                  (partition (count (:grid/parts rendered-grid))
                                             indexed-rows))
           [idx row] rows
           :let [[_ row-height] (math/rect-size (:row/rect row))]]
       (dom/div
        {:key idx
         :style {:height row-height}
         :class ["flex"
                 "justify-center"
                 "items-center"
                 "px-4"
                 "text-gray-400"
                 "text-sm"]}
        (when (zero? (mod idx (count (:grid/parts rendered-grid))))
          row-number))))))

(defnc ^:private HeaderRow [{:keys [rendered-grid]}]
  (dom/div
   {:class ["flex"]}
   (for [[idx header] (map-indexed vector (:grid/headers rendered-grid))]
     (dom/div
      {:key idx
       :style {:width (:grid/base-cell-size rendered-grid)}
       :class ["py-2" "text-center" "text-gray-400"]}
      header))))

(defnc Grid
  [{:keys [grid
           on-change

           selection
           on-selection-change

           pending-selection
           on-pending-selection-change

           editing
           on-edit]}]
  (let [rendered-grid (use-rendered-grid grid)
        [width height] (math/rect-size (:grid/rect rendered-grid))
        canvas-ref (hooks/use-ref nil)
        [context-menu-props ->context-menu-props] (hooks/use-state nil)]
    (hooks/use-effect
     [rendered-grid selection pending-selection]
     (draw! @canvas-ref rendered-grid selection pending-selection))
    (hooks/use-effect
     :auto-deps
     (when pending-selection
       (let [mouse-move-handler-fn
             (fn [^js event]
               (let [canvas-coord (util/offset-client-coord (.-current canvas-ref) (.-clientX event) (.-clientY event))
                     cell-coord (canvas-coord->cell-coord rendered-grid canvas-coord)
                     selection* (grid-selection/select-range pending-selection cell-coord)]
                 (when-not (= pending-selection selection*)
                   (on-pending-selection-change selection*))))
             mouse-up-handler-fn
             (fn [_]
               (on-selection-change pending-selection)
               (on-pending-selection-change nil))]
         (.addEventListener js/window "mousemove" mouse-move-handler-fn)
         (.addEventListener js/window "mouseup" mouse-up-handler-fn)
         (fn []
           (.removeEventListener js/window "mousemove" mouse-move-handler-fn)
           (.removeEventListener js/window "mouseup" mouse-up-handler-fn)))))
    (dom/div
     (dom/div
      {:class ["flex"]}
      (dom/div
       {:class ["mt-10"]}
       ($ RowLabelsColumn {:rendered-grid rendered-grid}))
      (dom/div
       ($ HeaderRow {:rendered-grid rendered-grid})
       (dom/div
        {:class ["relative"]}
        (dom/canvas
         {:ref canvas-ref
          :width (* (inc width) (.-devicePixelRatio js/window))
          :height (* (inc height) (.-devicePixelRatio js/window))
          :style {:width (inc width) :height (inc height)}
          :on-mouse-down
          (hooks/use-callback
           :auto-deps
           (fn [^js event]
             (when (zero? (.-button event))
               (let [canvas-coord (util/offset-client-coord (.-target event) (.-clientX event) (.-clientY event))
                     cell-coord (canvas-coord->cell-coord rendered-grid canvas-coord)
                     selection* (grid-selection/make-active-cell-selection cell-coord)]
                 (on-selection-change selection*)
                 (on-pending-selection-change selection*)
                 (when (and editing (not= (:cell-coord editing) cell-coord))
                   ;; Finish editing
                   (let [[part-idx row-idx cell-idx] (:cell-coord editing)]
                     (on-change
                      (if (string/blank? (:value editing))
                        (update-in grid [:grid/parts part-idx :part/rows row-idx :row/cells cell-idx] dissoc :cell/value)
                        (assoc-in grid [:grid/parts part-idx :part/rows row-idx :row/cells cell-idx :cell/value] (:value editing))))
                     (on-edit nil)))))))
          :on-double-click
          (hooks/use-callback
           [rendered-grid]
           (fn [^js event]
             (let [canvas-coord (util/offset-client-coord (.-target event) (.-clientX event) (.-clientY event))
                   [part-idx row-idx cell-idx :as cell-coord] (canvas-coord->cell-coord rendered-grid canvas-coord)]
               (on-edit {:cell-coord cell-coord
                         :value (get-in rendered-grid [:grid/parts part-idx :part/rows row-idx :row/cells cell-idx :cell/value])}))))
          :on-context-menu
          (hooks/use-callback
           :once
           (fn [^js event]
             (.preventDefault event)
             (->context-menu-props {:position [(.-clientX event) (.-clientY event)]})))})
        (when-let [{[part-idx row-idx cell-idx] :cell-coord :keys [value]} editing]
          ($ EditingCell
             {:cell (get-in rendered-grid [:grid/parts part-idx :part/rows row-idx :row/cells cell-idx])
              :value value
              :on-change #(on-edit (assoc editing :value %))}))))
      (when (some :part/title (:grid/parts rendered-grid))
        (dom/div
         {:class ["mt-10"]}
         ($ PartTitlesColumn {:rendered-grid rendered-grid})))
      (when context-menu-props
        ($ grid-context-menu/ContextMenu
           {:on-close #(->context-menu-props nil)
            :grid grid
            :on-grid-change on-change
            :selection selection
            :on-selection-change on-selection-change
            & context-menu-props}))))))
