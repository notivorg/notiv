(ns notiv.math
  (:require
   [goog.math :as gmath]))

(extend-type gmath/Rect
  IPrintWithWriter
  (-pr-writer [^goog.math/Rect r writer opts]
    (-write writer "#goog.math/Rect ")
    (pr-seq-writer [{:left (.-left r) :top (.-top r) :width (.-width r) :height (.-height r)}] writer opts)))

(defn ^goog.math/Rect rect
  "Returns a goog.math/Rect."
  [x y width height]
  (gmath/Rect. x y width height))

(defn rect-size
  "Returns a vector of [width height]."
  [^goog.math/Rect r]
  (let [size (.getSize r)]
    [(.-width size) (.-height size)]))

(defn rect-top-left [^goog.math/Rect r]
  (let [coord (.getTopLeft r)]
    [(.-x coord) (.-y coord)]))

(defn rect-bottom-right [^goog.math/Rect r]
  (let [coord (.getBottomRight r)]
    [(.-x coord) (.-y coord)]))

(defn rect-center [^goog.math/Rect r]
  (let [coord (.getCenter r)]
    [(.-x coord) (.-y coord)]))

(defn ^goog.math/Rect bounding-rect
  "Returns a goog.math/Rect containing both r1 and r2."
  [^goog.math/Rect r1 ^goog.math/Rect r2]
  (gmath/Rect.boundingRect r1 r2))

(defn rect-contains-point? [^goog.math/Rect r x y]
  (let [coord (gmath/Coordinate. x y)]
    (.contains r coord)))

(defn rect-contains-point-vertically? [^goog.math/Rect r x]
  (let [[left _] (rect-top-left r)
        [right _] (rect-bottom-right r)]
    (<= left x right)))

(defn rect-contains-point-horizontally? [^goog.math/Rect r y]
  (let [[_ top] (rect-top-left r)
        [_ bottom] (rect-bottom-right r)]
    (<= top y bottom)))

(defn rect-vertical-distance [^goog.math/Rect r y]
  (if (rect-contains-point-horizontally? r y)
    0
    (let [[_ top] (rect-top-left r)
          [_ bottom] (rect-bottom-right r)]
      (if (< y top)
        (- y top)
        (- y bottom)))))

(defn rect-horizontal-distance [^goog.math/Rect r x]
  (if (rect-contains-point-vertically? r x)
    0
    (let [[left _] (rect-top-left r)
          [right _] (rect-bottom-right r)]
      (if (< x left)
        (- x left)
        (- x right)))))

(defn gcd
  "Returns the greatest common divisor of x and y."
  [x y]
  (loop [x (Math/abs x)
         y (Math/abs y)]
    (if (zero? y)
      x
      (recur y (mod x y)))))

(defn lcm
  "Returns the least common multiple of x and y."
  [x y]
  (Math/abs (/ (* x y) (gcd x y))))
