(ns rtc.textfield
  (:require [reagent.core :as r]
            ["@material-ui/core" :as mui]
            [reagent.impl.template :as rtpl]))

;; see https://github.com/reagent-project/reagent/blob/master/examples/material-ui/src/example/core.cljs
(def ^:private input-component
  (r/reactify-component
    (fn [props]
      [:input (-> props
                  (assoc :ref (:inputRef props))
                  (dissoc :inputRef))])))

(def ^:private textarea-component
  (r/reactify-component
    (fn [props]
      [:textarea (-> props
                     (assoc :ref (:inputRef props))
                     (dissoc :inputRef))])))

(defn text-field [props & children]
  (let [props (-> props
                  (assoc-in [:InputProps :inputComponent] (cond
                                                            (and (:multiline props) (:rows props) (not (:maxRows props)))
                                                            textarea-component

                                                            ;; FIXME: Autosize multiline field is broken.
                                                            (:multiline props)
                                                            nil

                                                            ;; Select doesn't require cursor fix so default can be used.
                                                            (:select props)
                                                            nil

                                                            :else
                                                            input-component))
                  rtpl/convert-prop-value)]
    (apply r/create-element mui/TextField props (map r/as-element children))))
