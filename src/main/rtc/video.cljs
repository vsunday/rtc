(ns rtc.video
  (:require [goog.object :as gobj]
            [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]))

(declare local-video remote-video local-stream pc1 pc2
         on-ice-candidate on-ice-state-change got-remote-stream
         on-create-offer-success on-create-session-description-error
         on-set-session-description-error on-set-remote-success
         on-create-answer-success on-set-local-success
         on-add-ice-candidate-success on-add-ice-candidate-error)
;;(def local-video (.querySelector js/document "video#selfview"))
;;(def remote-video (.querySelector js/document "video#otherview"))
(def offer-options #js {:offer-to-receive-audio 1 :offer-to-receive-video 1})

(defn get-name [pc]
  (if (= pc pc1) "pc1" "pc2"))

(defn get-other-pc [pc]
  (if (= pc pc1) pc2 pc1))

(defn start []
  (.log js/console "Requesting local stream")
  (gobj/set (js/document.querySelector "button#start") "disabled" true)
  (go
    (try
      (let [stream (<p! (js/navigator.mediaDevices.getUserMedia #js {:audio true :video true}))]
        (js/console.log "Received local stream")
        ;;(gobj/set local-video "srcObject" stream)
        (gobj/set (js/document.querySelector "video#selfview") "srcObject" stream)
        (def local-stream stream)
        (gobj/set (js/document.querySelector "button#call") "disabled" false))
      (catch js/Error e (js/console.log (str "getUserMedia error: " e))))))

(defn call []
  (let [video-tracks (.getVideoTracks local-stream)
        audio-tracks (.getAudioTracks local-stream)
        configuration #js {}]
    (gobj/set (js/document.querySelector "button#call") "disabled" true)
    (gobj/set (js/document.querySelector "button#hangup") "disabled" false)

    (js/console.log "starting call")
    (when (> (.-length video-tracks) 0) (js/console.log (str "Using video device: " video-tracks)))
    (when (> (.-length audio-tracks) 0) (js/console.log (str "Using audio device: " audio-tracks)))
    (js/console.log (str "RTCPeerConnection config: " configuration))

    (def pc1 (js/RTCPeerConnection. configuration))
    (js/console.log "Created local peer connection object pc1")
    (.addEventListener pc1 "icecandidate" #(on-ice-candidate pc1 %))

    (def pc2 (js/RTCPeerConnection. configuration))
    (js/console.log "Created remote peer connection object pc2")
    (.addEventListener pc2 "icecandidate" #(on-ice-candidate pc2 %))
      
    (.addEventListener pc1 "iceconnectionstatechange" #(on-ice-state-change pc1 %))
    (.addEventListener pc2 "iceconnectionstatechange" #(on-ice-state-change pc2 %))
    (.addEventListener pc2 "track" got-remote-stream)

    (.forEach (.getTracks local-stream) #(.addTrack pc1 % local-stream))
    (js/console.log "Added local stream to pc")

    (try
      (js/console.log "pc1 createOffer start")
      (go
        (let [offer (<p! (.createOffer pc1 offer-options))]
          (on-create-offer-success offer)))
      (catch js/Error e (on-create-session-description-error e)))))

(defn on-create-session-description-error [error]
  (js/console.log (str "Failed to create session description: " (.toString error))))

(defn on-create-offer-success [desc]
  (js/console.log (str "Offer from pc1 " (.-sdp desc)))
  (js/console.log "pc1 setLocalDescription start")
  (try
    (go
      (<p! (.setLocalDescription pc1 desc))
      (on-set-local-success pc1))
    (catch js/Error e (on-set-session-description-error)))

  (js/console.log "pc2 setRemoteDescription start")
  (try
    (go
      (<p! (.setRemoteDescription pc2 desc))
      (on-set-remote-success pc2))
    (catch js/Error e (on-set-session-description-error)))

  (js/console.log "pc2 createAnswer start")
  (try
    (go
      (let [answer (<p! (.createAnswer pc2))]
        (on-create-answer-success answer)))
    (catch js/Error e (on-set-session-description-error))))

(defn on-set-local-success [pc]
  (js/console.log (str (get-name pc) " setLocalDescription complete")))

(defn on-set-remote-success [pc]
  (js/console.log (str (get-name pc) " setRemoteDescription complete")))

(defn on-set-session-description-error [error]
  (js/console.log (str "Failed to set session description: " (.toString error))))

(defn got-remote-stream [e]
  (let [remote-video (js/document.querySelector "video#otherview")]
    (when (not= (.-srcObject remote-video) (aget (.-streams e) 0))
      (do
        (gobj/set remote-video "srcObject" (aget (.-streams e) 0))
        (js/console.log "pc2 received remote stream")))))

(defn on-create-answer-success [desc]
  (do
    (js/console.log (str "Answer from pc2: " (.-sdp desc)))
    (go
      (js/console.log "pc2 setLocalDescription start")
      (try
        (<p! (.setLocalDescription pc2 desc))
        (on-set-local-success pc2)
        (catch js/Error e (on-set-session-description-error e)))

      (js/console.log "pc1 setRemoteDescription start")
      (try
        (<p! (.setRemoteDescription pc1 desc))
        (on-set-remote-success pc1)
        (catch js/Error e (on-set-session-description-error e))))))

(defn on-ice-candidate [pc event]
  (go
    (try
      (<p! (.addIceCandidate (get-other-pc pc) (.-candidate event)))
      (on-add-ice-candidate-success pc)
      (catch js/Error e (on-add-ice-candidate-error pc e))))
  (js/console.log (str (get-name pc) " ICE candidate: " (if (some? (.-candidate event)) (.-candidate (.-candidate event)) "null"))))

(defn on-add-ice-candidate-success [pc]
  (js/console.log (str (get-name pc) " addIceCandidate success")))

(defn on-add-ice-candidate-error [pc error]
  (js/console.log (str (get-name pc) " failed to add ICE Candidate: " (.toString error))))

(defn on-ice-state-change [pc event]
  (when (some? pc)
    (do
      (js/console.log (str (get-name pc) " ICE state: " (.-iceConnectionState pc)))
      (js/console.log (str "ICE state change event: " event)))))

(defn hangup []
  (js/console.log "Ending call")
  (.close pc1)
  (.close pc2)
  (ns-unmap 'rtc.video 'pc1)
  (ns-unmap 'rtc.video 'pc2)
  (gobj/set (js/document.querySelector "button#hangup") "disabled" true)
  (gobj/set (js/document.querySelector "button#call") "disabled" false))
