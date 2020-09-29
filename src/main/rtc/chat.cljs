(ns rtc.chat
  (:require [goog.object :as gobj]
            ;; [cljs.core.async :refer [go]]
            ;; [cljs.core.async.interop :refer-macros [<p!]]
            ))

;; https://github.com/webrtc/samples/blob/gh-pages/src/content/datachannel/basic/js/main.js
(declare local-conn remote-conn send-chan receive-chan
         on-ice-candidate on-send-channel-state-change receive-channel-callback
         got-description1 got-description2 on-create-session-description-error
         on-receive-message-callback on-receive-channel-state-change)

(def data-channel-send
  (.querySelector js/document "input[type=\"text\"]"))

(defn create-conn []
  (let [servers nil]
    (def local-conn (js/RTCPeerConnection. servers))
    (gobj/set js/window "localConnection" local-conn)
    (.log js/console "Created local peer connection object local-conn")

    (def send-chan (.createDataChannel local-conn "sendDataChannel"))
    (.log js/console "Created send data channel")
    
    (gobj/set local-conn "onicecandidate"  #(on-ice-candidate local-conn %))
;;    (gobj/set local-conn "onicecandidate" (fn [e] (on-ice-candidate local-conn e)))
    (gobj/set send-chan "onopen" on-send-channel-state-change)
    (gobj/set send-chan "onclose" on-send-channel-state-change)
    
    (def remote-conn (js/RTCPeerConnection. servers))
    (gobj/set js/window "remoteConnection"  remote-conn)
    (.log js/console "Created remote peer connection object remote-conn")

    (gobj/set remote-conn "onicecandidate"  #(on-ice-candidate remote-conn %))
;;    (gobj/set remote-conn "onicecandidate" (fn [e] (on-ice-candidate remote-conn e)))
    (gobj/set remote-conn "ondatachannel" receive-channel-callback)    

    ;; https://clojurescript.org/guides/promise-interop
    (.then (.createOffer local-conn) got-description1 on-create-session-description-error)
    ))

(defn on-create-session-description-error [error]
  (.log js/console (str "Failed to create session description: " (.toString error))))

(defn send-data [data]
  (do
    (.send send-chan data)
    (.log js/console (str "sent data: " data))))

(defn got-description1 [desc]
  (do
    (.setLocalDescription local-conn desc)
    (.log js/console (str "Offer from local-conn\n" (.-sdp desc)))
    (.setRemoteDescription remote-conn desc)
    (.then (.createAnswer remote-conn) got-description2 on-create-session-description-error)))

(defn got-description2 [desc]
  (do
    (.setLocalDescription remote-conn desc)
    (.log js/console (str "Answer from remote-conn\n" (.-sdp desc)))
    (.setRemoteDescription local-conn desc)))

(defn get-other-pc [pc]
  (if (= pc local-conn) remote-conn local-conn))

(defn get-name [pc]
  (if (= pc local-conn) "local-peer-conn" "remote-peer-conn"))

(defn on-add-ice-candidate-success []
  (.log js/console "add-ice-candidate success"))

(defn on-add-ice-candidate-error [error]
  (.log js/console (str "Failed to add candidate: " (.toString error))))

(defn on-ice-candidate [pc event]
  (do 
    (->  (get-other-pc pc)
         (.addIceCandidate (.-candidate event))
         (.then on-add-ice-candidate-success on-add-ice-candidate-error))
    (.log js/console (str (get-name pc) " ICE candidate: " (if (some? (.-candidate event)) (.-candidate (.-candidate event)) "null")))
))

(defn receive-channel-callback [event]
  (do
    (.log js/console "Receive channel Callback")
    (def receive-chan (.-channel event))
    (gobj/set receive-chan "onmessage" on-receive-message-callback)
    (gobj/set receive-chan "onopen" on-receive-channel-state-change)
    (gobj/set receive-chan "onclose" on-receive-channel-state-change)))

(defn on-receive-message-callback [event]
  (.log js/console (str "Received Message: " (.-data event))))

(defn on-send-channel-state-change []
  (let [ready-state (.-readyState send-chan)]
    (.log js/console (str "Send channel state is: " ready-state))))

(defn on-receive-channel-state-change []
  (let [ready-state (.-readyState receive-chan)]
    (.log js/console (str "Receive channel state is: " ready-state))))

(create-conn)
