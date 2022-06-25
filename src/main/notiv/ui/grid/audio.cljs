(ns notiv.ui.grid.audio
  (:require
   ["tone" :as Tone]))

(defn- bpm-beat-duration [bpm]
  (/ 1 (/ bpm 60)))

(defn- grid-col-duration [grid]
  (/ (bpm-beat-duration (:grid/bpm grid))
     (:grid/base-cells-per-beat grid)
     (:grid/base-cell-col-span grid)))

(defn- render-part [grid part]
  (let [col-duration (grid-col-duration grid)
        cells (mapcat :row/cells (:part/rows part))
        next-time (volatile! 0)
        events (keep
                (fn [cell]
                  (let [time @next-time]
                    (vreset! next-time (+ time (* col-duration (:cell/col-span cell))))
                    (when (:cell/pitches cell)
                      [time (:cell/pitches cell)])))
                cells)
        synth (.toDestination (Tone/Synth.))]
    (Tone/Part.
     (fn [time pitches]
       (doseq [[pitch-class octave] pitches]
         (-> (Tone/Synth.)
             (.toDestination)
             (.triggerAttackRelease (str pitch-class octave) "16n" time))))
     (into-array (map into-array events)))))

(defn render-parts [parsed-grid]
  (mapv (partial render-part parsed-grid) (:grid/parts parsed-grid)))
