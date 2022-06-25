(ns notiv.ui.grid.parse
  (:require
   [clojure.string :as string]
   [instaparse.core :as instaparse]
   [notiv.music :as music])
  (:require-macros
   [instaparse.core :as instaparse]))

(instaparse/defparser cell-value-parser
  "
cell-value = absolute-pitch | scale-degree | chord-numeral | chord-name

absolute-pitch = pitch-class pos-int

scale-degree = octave-transposition? accidental? pos-int

chord-numeral = accidental? ( major-chord-numeral | minor-chord-numeral )
major-chord-numeral = 'I' | 'II' | 'III' | 'IV' | 'V' | 'VI' | 'VII'
minor-chord-numeral = 'i' | 'ii' | 'iii' | 'iv' | 'v' | 'vi' | 'vii'

chord-name = pitch-class chord-type
chord-type = 'Maj' | 'min'

pitch-class = 'C' | 'C#' | 'D' | 'D#' | 'E' | 'F' | 'F#' | 'G' | 'G#' | 'A' | 'A#' | 'B'

octave-transposition = '+' | '-'
accidental = 'b' | '#'

pos-int = #'[0-9]+'
")

(def major-scale-intervals [0 2 4 5 7 9 11])

(def chord-type->intervals
  {"Maj" [0 4 7]
   "min" [0 3 7]})

(defn parse-roman-numeral [s]
  (case (string/lower-case s)
    "i" 1
    "ii" 2
    "iii" 3
    "iv" 4
    "v" 5
    "vi" 6
    "vii" 7))

(defn scale-degree->pitch [n semitones-offset {:keys [tonic octave]}]
  (let [interval (+ (nth major-scale-intervals (mod (dec n) (count major-scale-intervals)))
                    (* 12 (quot (dec n) (count major-scale-intervals)))
                    semitones-offset)]
    (music/add-semitones [tonic octave] interval)))

(defn parse-cell-value [s {:keys [tonic octave]}]
  (->> (instaparse/parse cell-value-parser s)
       (instaparse/transform
        {:pos-int
         (fn [n]
           [:pos-int (js/parseInt n)])

         :absolute-pitch
         (fn [[_ pitch-class] [_ octave*]]
           {:cell/pitches [[pitch-class octave*]]
            :octave octave})

         :octave-transposition
         (fn [s]
           [:octave-transposition
            (case s
              "+" 1
              "-" -1)])

         :accidental
         (fn [s]
           [:accidental
            (case s
              "b" -1
              "#" 1)])

         :scale-degree
         (fn
           ([[_ n]]
            {:cell/pitches [(scale-degree->pitch n 0 {:tonic tonic :octave octave})]
             :octave octave})

           ([[tag x] [_ n]]
            (case tag
              :octave-transposition
              (let [transposed-octave (+ octave x)]
                {:cell/pitches [(scale-degree->pitch n 0 {:tonic tonic :octave transposed-octave})]
                 :octave transposed-octave})

              :accidental
              {:cell/pitches [(scale-degree->pitch n x {:tonic tonic :octave octave})]
               :octave octave}))

           ([[_ octave-transposition] [_ semitones-offset] [_ n]]
            (let [transposed-octave (+ octave octave-transposition)]
              {:cell/pitches [(scale-degree->pitch n semitones-offset {:tonic tonic :octave transposed-octave})]
               :octave transposed-octave})))

         :major-chord-numeral (fn [s] ["Maj" s])

         :minor-chord-numeral (fn [s] ["min" s])

         :chord-numeral
         (fn
           ([[chord-type chord-numeral]]
            (let [n (parse-roman-numeral chord-numeral)
                  root (scale-degree->pitch n 0 {:tonic tonic :octave octave})]
              {:cell/pitches (mapv #(music/add-semitones root %) (chord-type->intervals chord-type))
               :octave octave}))

           ([[_ semitones-offset] [chord-type chord-numeral]]
            (let [n (parse-roman-numeral chord-numeral)
                  root (scale-degree->pitch n semitones-offset {:tonic tonic :octave octave})]
              {:cell/pitches (mapv #(music/add-semitones root %) (chord-type->intervals chord-type))
               :octave octave})))

         :chord-name
         (fn [[_ pitch-class] [_ chord-type]]
           (let [root [pitch-class octave]]
             {:cell/pitches (mapv #(music/add-semitones root %) (chord-type->intervals chord-type))
              :octave octave}))

         :cell-value identity})))

(defn parse-row [row {:keys [tonic octave]}]
  (let [current-octave (volatile! octave)]
    (update row
            :row/cells
            (fn [cells]
              (mapv (fn [cell]
                      (if (:cell/value cell)
                        (let [result (parse-cell-value (:cell/value cell) {:tonic tonic :octave @current-octave})]
                          (if (instaparse/failure? result)
                            (assoc cell :cell/parse-error true)
                            (let [{pitches :cell/pitches next-octave :octave} result]
                              (vreset! current-octave next-octave)
                              (assoc cell :cell/pitches pitches))))
                        cell))
                    cells)))))

(defn parse-part [part]
  (update part
          :part/rows
          (fn [rows]
            (mapv (fn [row]
                    (parse-row row {:tonic (:part/tonic part) :octave (:part/octave part)}))
                  rows))))

(defn parse-grid [grid]
  (update grid :grid/parts #(mapv parse-part %)))
