(ns notiv.ui.grid.parse-test
  (:require
   [cljs.test :refer [deftest testing is are]]
   [notiv.ui.grid.parse :as grid-parse]))

(deftest parse-cell-value-test
  (testing "Scale degrees"
    (are [expected value] (= expected (:cell/pitches (grid-parse/parse-cell-value value {:tonic "C" :octave 4})))
      [["C" 4]] "1"
      [["D" 4]] "2"
      [["C" 5]] "8"
      [["D" 5]] "9"
      [["E" 5]] "10"
      [["B" 5]] "14"
      [["C" 6]] "15"
      [["D" 6]] "16"))

  (testing "Scale degrees with accidentals"
    (are [expected value] (= expected (:cell/pitches (grid-parse/parse-cell-value value {:tonic "C" :octave 4})))
      [["C#" 4]] "b2"
      [["D#" 4]] "#2"
      [["A#" 4]] "b7"
      [["C#" 5]] "b9"
      [["D#" 5]] "#9"))

  (testing "Scale degrees with octave transposition"
    (are [expected value] (= expected (grid-parse/parse-cell-value value {:tonic "C" :octave 4}))
      {:cell/pitches [["C" 5]] :octave 5} "+1"
      {:cell/pitches [["C" 3]] :octave 3} "-1"))

  (testing "Absolute pitches"
    (are [expected value] (= expected (:cell/pitches (grid-parse/parse-cell-value value {:tonic "C" :octave 4})))
      [["C" 4]] "C4"
      [["C" 0]] "C0"
      [["C" 16]] "C16"))

  (testing "Chord numerals"
    (are [expected value] (= expected (:cell/pitches (grid-parse/parse-cell-value value {:tonic "C" :octave 4})))
      [["C" 4] ["E" 4] ["G" 4]] "I"
      [["D" 4] ["F" 4] ["A" 4]] "ii"))

  (testing "Chord names"
    (are [expected value] (= expected (:cell/pitches (grid-parse/parse-cell-value value {:tonic "C" :octave 4})))
      [["C" 4] ["E" 4] ["G" 4]] "CMaj"
      [["D" 4] ["F" 4] ["A" 4]] "Dmin")))

(deftest parse-grid-test
  (testing "Octave transposition"
    (let [parsed-grid (grid-parse/parse-grid
                       {:grid/parts
                        [{:part/tonic "C"
                          :part/octave 4
                          :part/rows
                          [{:row/cells [{:cell/value "1"} {:cell/value "+1"}]}
                           {:row/cells [{:cell/value "1"} {:cell/value "+1"}]}]}]})]
      (is (= [["C" 4]] (get-in parsed-grid [:grid/parts 0 :part/rows 0 :row/cells 0 :cell/pitches])))
      (is (= [["C" 5]] (get-in parsed-grid [:grid/parts 0 :part/rows 0 :row/cells 1 :cell/pitches])))
      (is (= [["C" 4]] (get-in parsed-grid [:grid/parts 0 :part/rows 1 :row/cells 0 :cell/pitches]))
          "Octave should reset on the next row")
      (is (= [["C" 5]] (get-in parsed-grid [:grid/parts 0 :part/rows 1 :row/cells 1 :cell/pitches]))))))
