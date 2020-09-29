(ns rtc.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            ["@material-ui/core" :as mui]
            ["@material-ui/core/styles" :as mstyles]
            ["@material-ui/icons" :as micons]
            [reagent.impl.template :as rtpl]
            [rtc.textfield :refer [text-field]]
            [rtc.chat :as chat]
            [rtc.sign :as sign]
            [rtc.video :as video]))

(defn camera-view
  ([]
   [:video#otherview {:plays-inline true :auto-play true :style {:background-color "gray" :width "100%" :max-width "initial" :max-height "initial"}}])
  ([w]
   [:video#selfview {:plays-inline true :auto-play true :muted true :style {:background-color "gray" :width w :max-width "initial" :max-height "initial"}}]))

(def talks
  (r/atom [{:who 1 :msg "ようこそ!"}
           {:who 1 :msg "チャットでも会話ができます"}]))

(comment "(reset! talks
                 (vec
                  (for [n (range 10)]
                    {:who 1 :msg (str \"sample message \" n)})))")

(defn chat-chip [talk]
  (let [[align color who] (if (zero? (:who talk)) ["right" "primary" "あなた"] ["left" "default" "オペレータ"])]
    [:> mui/Grid {:item true :xs 12 :display "flex" :style {:text-align align}}
;;     [:> mui/Typography {:variant "body2"} who]
     [:> mui/Chip {:label (:msg talk) :color color}]]))

(defn chat-view []
  (let [text (r/atom "")
        send (fn []
               (when (not= "" @text)
                 (do
                   (swap! talks conj {:who 0 :msg @text})
                   (.setTimeout js/window #(swap! talks conj {:who 1 :msg (:msg (last @talks))}) (+ 500 (rand-int 1500)))
                   (reset! text "")
                   (chat/send-data @talks))))]
    (fn []
      [:> mui/Grid {:m 1 :container true}
       [:> mui/Paper {:style {:width "100%" :height "90%" :max-height "80vh" :overflow-y "scroll" :overflow-x "hidden" :background-color "#bbbbbb"} :elevation 2} 
        [:> mui/Grid {:container true :spacing 1}
         (for [[n talk] (map-indexed vector @talks)]
           ^{:key n} [chat-chip talk])]]
       [:> mui/Grid {:container true :style {:height "10%"}}
        [:> mui/Grid {:item true :xs 9}
         [text-field {:value @text :variant "filled" :style {:width "100%"}
                            :on-change #(reset! text (-> % .-target .-value))
                            :on-key-down #(case (.-which %)
                                            13 (send)
                                            nil)}]]
        [:> mui/Grid {:item true :xs 3}
         [:> mui/IconButton {:on-click send}
          [:> micons/Send]]]]])))

(defn main []
  [:div {:style {:background-color "#eeeeee" :max-height "80vh"}}
   [:> mui/AppBar {:style {:margin-bottom 3} :position "static"}
    [:> mui/Toolbar
     [:> mui/IconButton {:edge "start"}
      [:> micons/Menu]]
     [:> mui/Typography "RTC Demo"]]]
   [:> mui/Grid {:container true :spacing 1}
    [:> mui/Grid {:container true :item true :xs 9}
     [:> mui/Grid {:item true :xs 12}
      [camera-view]]
     [:> mui/Box {:style {:width "100%"} :display "flex" :justify-content "center"}
      [camera-view "40%"]]]
    [:> mui/Grid {:container true :item true :xs 3 :style {:height "inherit"}}
     [chat-view]]]
   [:button#start {:on-click video/start :disabled false} "start"]
   [:button#call {:on-click video/call} "call"]
   [:button#hangup {:on-click video/hangup} "hangup"]])

(defn init []
  (rdom/render [main] (js/document.getElementById "app")))

(init)
