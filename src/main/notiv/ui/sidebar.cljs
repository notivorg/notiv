(ns notiv.ui.sidebar
  (:require
   ["react" :as React]
   [helix.core :as helix :refer [defnc $]]
   [helix.dom :as dom]
   ["@heroicons/react/solid" :as heroicons-solid]
   [helix.hooks :as hooks]
   [notiv.music :as music]
   [clojure.string :as string]))

(defn use-id [prefix]
  (hooks/use-memo [prefix] (str (gensym prefix))))

(defnc NumberInput [{:keys [on-change] :as props}]
  (dom/input
   {:type "number"
    :on-change
    (fn [^js event]
      (let [n (js/parseInt (.. event -target -value))]
        (when-not (js/isNaN n)
          (on-change n))))
    & (dissoc props :on-change)}))

(defnc Setting [{:keys [label input]}]
  (let [id (use-id "SettingInput")]
    (dom/div
     {:class ["flex" "justify-between"]}
     (dom/label {:class ["text-white"] :for id} label)
     (React/cloneElement input #js {:id id}))))

(defnc Part [{:keys [part on-change on-delete delete-disabled]}]
  (let [[editing-title ->editing-title] (hooks/use-state false)
        title-form-id (use-id "PartTitleForm")]
    (dom/div
     {:class ["border"
              "border-gray-600"
              "rounded-lg"]}
     (dom/div
      {:class ["px-2"
               "py-1"
               "flex"
               "justify-between"
               "items-center"
               "bg-gray-600"
               "rounded-t-lg"]}
      (if editing-title
        (dom/form
         {:id title-form-id
          :on-submit
          (fn [^js event]
            (.preventDefault event)
            (->editing-title false)
            (let [title (.. event -target -elements -title -value)]
              (if (string/blank? title)
                (on-change (dissoc part :part/title))
                (on-change (assoc part :part/title title)))))
          :on-blur
          (fn []
            (->editing-title false))}
         (dom/input
          {:class ["w-full"]
           :type "text"
           :name "title"
           :default-value (:part/title part)
           :placeholder "Part"
           :auto-focus true}))
        (dom/div
         {:class ["font-medium" "text-white"]
          :on-click #(->editing-title true)}
         (or (:part/title part)
             "Part")))
      (dom/div
       {:class ["flex" "items-center" "text-white"]}
       (if editing-title
         (dom/button
          {:type "submit"
           :form title-form-id}
          ($ ^:native heroicons-solid/CheckIcon
             {:class ["w-5"
                      "h-5"]}))
         (dom/button
          {:on-click (fn [] (js/setTimeout (fn [] (->editing-title true)) 50))}
          ($ ^:native heroicons-solid/PencilIcon
             {:class ["w-5"
                      "h-5"]})))
       (when-not delete-disabled
         (dom/button
          {:class ["ml-2"]}
          ($ ^:native heroicons-solid/TrashIcon
             {:class ["w-5"
                      "h-5"]
              :on-click #(on-delete)})))))
     (dom/div
      {:class ["p-2"]}
      (dom/div
       ($ Setting
          {:label "Tonic"
           :input (dom/select
                   {:class ["w-16"]
                    :value (:part/tonic part)
                    :on-change
                    (fn [^js event]
                      (on-change (assoc part :part/tonic (.. event -target -value))))}
                   (for [pitch-class music/pitch-classes]
                     (dom/option {:key pitch-class :value pitch-class} pitch-class)))}))
      (dom/div
       {:class ["mt-2"]}
       ($ Setting
          {:label "Octave"
           :input ($ NumberInput
                     {:class ["w-16"]
                      :value (:part/octave part)
                      :on-change
                      (fn [octave]
                        (on-change (assoc part :part/octave octave)))})}))))))

(defnc SettingsSidebar [{:keys [grid on-change]}]
  (dom/div
   {:class ["fixed"
            "top-0"
            "right-0"
            "h-full"
            "w-[18rem]"
            "bg-gray-700"
            "border-l"
            "border-gray-600"]}
   (dom/div
    {:class ["text-white"
             "font-semibold"
             "border-b"
             "border-gray-600"]}
    (dom/div
     {:class ["flex"
              "justify-between"
              "items-center"]}
     (dom/div
      {:class ["px-4"
               "py-2"]}
      "Playback")))
   (dom/div
    {:class ["p-4"]}
    (dom/div
     ($ Setting
        {:label "BPM"
         :input ($ NumberInput
                   {:class ["w-16"]
                    :value (:grid/bpm grid)
                    :on-change
                    (fn [bpm]
                      (on-change (assoc grid :grid/bpm bpm)))})}))
    (dom/div
     {:class ["mt-2"]}
     ($ Setting
        {:label "Base cells per beat"
         :input ($ NumberInput
                   {:class ["w-16"]
                    :value (:grid/base-cells-per-beat grid)
                    :on-change
                    (fn [n]
                      (on-change (assoc grid :grid/base-cells-per-beat n)))})})))
   (dom/div
    {:class ["text-white"
             "font-semibold"
             "border-b"
             "border-gray-600"]}
    (dom/div
     {:class ["flex"
              "justify-between"
              "items-center"]}
     (dom/div
      {:class ["px-4"
               "py-2"]}
      "Parts")
     (dom/button
      {:class ["px-4"
               "py-2"
               "text-white"
               "hover:text-blue-400"]
       :on-click
       (fn []
         (on-change (update grid :grid/parts conj (last (:grid/parts grid)))))}
      ($ ^:native heroicons-solid/PlusCircleIcon
         {:class ["w-5"
                  "h-5"]}))))
   (for [[idx part] (map-indexed vector (:grid/parts grid))]
     (dom/div
      {:key idx
       :class ["p-2"]}
      ($ Part
         {:part part
          :delete-disabled (= 1 (count (:grid/parts grid)))
          :on-change
          (fn [next-part]
            (on-change (assoc-in grid [:grid/parts idx] next-part)))
          :on-delete
          (fn []
            (when (> (count (:grid/parts grid)) 1)
              (on-change (update grid :grid/parts (fn [parts] (into (subvec parts 0 idx) (subvec parts (inc idx))))))))})))))
