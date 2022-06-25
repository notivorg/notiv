(ns notiv.music)

(def pitch-classes ["C" "C#" "D" "D#" "E" "F" "F#" "G" "G#" "A" "A#" "B"])

(def pitch-class->idx (zipmap pitch-classes (range)))

(defn pitches-seq
  "Returns a lazy infinite seq of pitches ascending in semitones from start-pitch, inclusive."
  [start-pitch]
  (let [[start-pitch-class start-octave] start-pitch
        octave-boundary (= start-pitch-class (last pitch-classes))
        pitch-class-idx (pitch-class->idx start-pitch-class)
        next-pitch-class (nth pitch-classes (mod (inc pitch-class-idx) (count pitch-classes)))
        next-octave (if octave-boundary (inc start-octave) start-octave)]
    (lazy-seq (cons [start-pitch-class start-octave] (pitches-seq [next-pitch-class next-octave])))))

(defn add-semitones
  "Adds an interval of n semitones to pitch."
  [pitch n]
  (nth (pitches-seq pitch) n))
