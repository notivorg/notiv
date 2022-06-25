(ns notiv.main
  (:require
   [lambdaisland.glogi :as log]
   [lambdaisland.glogi.console :as glogi-console]
   [helix.core :as helix :refer [defnc $]]
   [helix.dom :as dom]
   [helix.hooks :as hooks]
   ["react-dom" :as ReactDOM]
   ["@heroicons/react/solid" :as heroicons-solid]
   ["tone" :as Tone]
   [notiv.ui.context-menu :as context-menu]
   [notiv.ui.grid :as grid]
   [notiv.ui.grid.audio :as grid-audio]
   [notiv.ui.grid.parse :as grid-parse]
   [notiv.ui.sidebar :as sidebar]))

(glogi-console/install!)

(log/set-levels {:glogi/root :info})

(defnc ^:private GridPlayButton [props]
  (dom/button
   {:class ["pl-4"
            "pr-2"
            "py-1"
            "bg-blue-500"
            "bg-opacity-50"
            "hover:bg-opacity-100"
            "text-white"
            "rounded-full"
            "font-medium"]
    & props}
   "Play"
   ($ ^:native heroicons-solid/PlayIcon
      {:class ["inline"
               "w-6"
               "h-6"
               "ml-2"
               "-mt-[3px]"]})))

(defnc ^:private TestGrid [{:keys [grid on-change]}]
  (let [[selection ->selection] (hooks/use-state nil)
        [pending-selection ->pending-selection] (hooks/use-state nil)
        [editing ->editing] (hooks/use-state nil)]
    (dom/div
     {:class ["inline-flex" "m-6"]}
     (dom/div
      ($ grid/Grid
         {:grid grid
          :on-change on-change
          :selection selection
          :on-selection-change ->selection
          :pending-selection pending-selection
          :on-pending-selection-change ->pending-selection
          :editing editing
          :on-edit ->editing}))
     (dom/div
      {:class ["mt-10" "pl-6"]}
      ($ GridPlayButton
         {:on-click
          (fn []
            (let [parts (grid-audio/render-parts (grid-parse/parse-grid grid))]
              (.start Tone/Transport)
              (doseq [part parts]
                (.start ^js part))))})))))

(def ^:private initial-test-grid
  {:grid/base-cell-col-span 1
   :grid/base-cell-size 48
   :grid/headers ["1" "e" "&" "a" "2" "e" "&" "a" "3" "e" "&" "a" "4" "e" "&" "a"]
   :grid/bpm 100
   :grid/base-cells-per-beat 4
   :grid/parts
   [{:part/tonic "C"
     :part/octave 4
     :part/rows [{:row/cells (vec (repeat 16 {:cell/col-span 1}))}
                 {:row/cells (vec (repeat 16 {:cell/col-span 1}))}
                 {:row/cells (vec (repeat 16 {:cell/col-span 1}))}
                 {:row/cells (vec (repeat 16 {:cell/col-span 1}))}]}]})

(defonce ^:private grid-ref (atom initial-test-grid))

(defn- use-ref-subscription [ref]
  (hooks/use-subscription
   {:get-current-value (hooks/use-callback [ref] #(deref ref))
    :subscribe
    (hooks/use-callback
     [ref]
     (fn [on-update]
       (let [k (gensym "use-ref-subscription")]
         (add-watch ref k on-update)
         (fn []
           (remove-watch ref k)))))}))

(defnc ^:private Root []
  (let [context-menu-container-ref (hooks/use-ref nil)
        grid (use-ref-subscription grid-ref)]
    (helix/provider
     {:context context-menu/context-menu-portal-context
      :value context-menu-container-ref}
     ($ TestGrid {:grid grid :on-change (hooks/use-callback :once #(reset! grid-ref %))})
     ($ sidebar/SettingsSidebar
        {:grid grid
         :on-change (hooks/use-callback :once #(reset! grid-ref %))})
     (dom/div {:ref context-menu-container-ref}))))

(defn- ^:dev/after-load mount-root! []
  (ReactDOM/render ($ Root) (.getElementById js/document "root")))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn main []
  (mount-root!))
