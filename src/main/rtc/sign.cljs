(ns rtc.sign
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            ["@material-ui/core" :as mui]
            ["@material-ui/core/styles" :as mstyles]
            ["@material-ui/icons" :as micons]
            [goog.object :as gobj]
            [reagent.impl.template :as rtpl]))

(defn sign-area []
  (let [is-drawing (r/atom false)
        x (r/atom 0)
        y (r/atom 0)
        context (fn [] (.getContext (.querySelector js/document "canvas#sign") "2d"))
        draw-line (fn
                    [context x1 y1 x2 y2]
                    (do
                      (.beginPath context)
                      (gobj/set context "storkeStyle" "black")
                      (gobj/set context "lineWidth" 1)
                      (.moveTo context x1 y1)
                      (.lineTo context x2 y2)
                      (.stroke context)
                      (.closePath context)))
        ;; https://stackoverflow.com/questions/31519758/reacts-mouseevent-doesnt-have-offsetx-offsety
        getX #(.-offsetX (.-nativeEvent %))
        getY #(.-offsetY (.-nativeEvent %))]
    [:div
     [:h1 "sign test"]
     [:canvas#sign {:height 100 :width 200 :style {:border "1px solid black"}
                    :on-mouse-down #(do
                                      (reset! x (getX %))
                                      (reset! y (getY %))
                                      (swap! is-drawing not))
                    :on-mouse-move #(when @is-drawing (do
                                                        (draw-line (context) @x @y (getX %) (getY %))
                                                        (reset! x (getX %))
                                                        (reset! y (getY %))))
                    :on-mouse-up   #(when @is-drawing (do
                                                        (draw-line (context) @x @y (getX %) (getY %))
                                                        (reset! x 0)
                                                        (reset! y 0)
                                                        (swap! is-drawing not)))}]]))

(defn reset-button []
  (let [canvas (.querySelector js/document "canvas#sign")
        clear (fn []
                (-> (.getContext canvas "2d")
                    (.clearRect 0 0 (.-width canvas) (.-height canvas))))]
    [:div
     [:> mui/Button {:color "secondary" :on-click clear} "reset"]]))

(defn sign []
  [:div
   [sign-area]
   [reset-button]])
