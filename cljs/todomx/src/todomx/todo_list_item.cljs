(ns todomx.todo-list-item
  (:require [cljs.pprint :as pp]
            [clojure.string :as str]
            [bide.core :as r]
            [taoensso.tufte :as tufte :refer-macros (defnp profiled profile)]

            [tiltontec.util.core :refer [pln xor now]]
            [tiltontec.cell.base :refer [unbound ia-type *within-integrity* *defer-changes*]]
            [tiltontec.cell.core :refer-macros [c? c?+ c?n c?+n c?once] :refer [c-in]]
            [tiltontec.cell.evaluate :refer [not-to-be]]
            [tiltontec.cell.observer :refer-macros [fn-obs]]
            [tiltontec.cell.synapse
             :refer-macros [with-synapse]
             :refer []]


            [tiltontec.model.core :refer [matrix mx-par <mget mset!> mswap!>
                                          fget mxi-find mxu-find-class mxu-find-type
                                          kid-values-kids] :as md]
            [tiltontec.tag.html
             :refer [io-read io-upsert io-clear-storage
                     tag-dom-create
                     mxu-find-tag mxu-find-class
                     dom-tag tagfo tag-dom
                     dom-has-class dom-ancestor-by-tag]
             :as tag]

            [tiltontec.xhr
             :refer [make-xhr send-xhr send-unparsed-xhr xhr-send xhr-await xhr-status
                     xhr-status-key xhr-resolved xhr-error xhr-error? xhrfo synaptic-xhr synaptic-xhr-unparsed
                     xhr-selection xhr-to-map xhr-name-to-map xhr-response]]

            [tiltontec.tag.gen
             :refer-macros [section header h1 input footer p a
                            span label ul li div button br]
             :refer [dom-tag evt-tag]]

            [tiltontec.tag.style :refer [make-css-inline]]

            [goog.dom :as dom]
            [goog.dom.classlist :as classlist]
            [goog.editor.focus :as focus]
            [goog.dom.selection :as selection]
            [goog.events.Event :as event]
            [goog.dom.forms :as form]

            [todomx.todo
             :refer [make-todo td-title td-created bulk-todo
                     td-completed td-upsert td-delete! load-all
                     td-id td-toggle-completed! td-due-by]]
            [cljs-time.coerce :as tmc]
            [clojure.string :as $]))

(declare todo-edit
         ae-explorer
         due-by-input)

(defn todo-list-item [me todo matrix]
  (let [ul-tag me]
    (li {:class   (c? [(when (<mget me :selected?) "chosen")
                       (when (<mget me :editing?) "editing")
                       (when (td-completed todo) "completed")])

         :display (c? (if-let [route (<mget matrix :route)]
                        (cond
                          (or (= route "All")
                              (xor (= route "Active")
                                   (td-completed todo))) "block"
                          :default "none")
                        "block"))}
        ;;; custom slots..
        {:todo      todo
         ;; above is also key to identify lost/gained LIs, in turn to optimize list maintenance

         :selected? (c? (some #{todo} (<mget ul-tag :selections)))

         :editing?  (c-in false)}

        (let [todo-li me]
          [(div {:class "view"}
             (input {:class   "toggle" ::tag/type "checkbox"
                     :checked (c? (not (nil? (td-completed todo))))
                     :onclick #(td-toggle-completed! todo)})

             (label {:onclick    (fn [evt]
                                   (mswap!> ul-tag :selections
                                            #(if (some #{todo} %)
                                               (remove #{todo} %)
                                               (conj % todo))))

                     :ondblclick #(do
                                    (mset!> todo-li :editing? true)
                                    (tag/input-editing-start
                                      (dom/getElementByClass "edit" (tag-dom todo-li))
                                      (td-title todo)))}
                    (td-title todo))

             (due-by-input todo)

             (ae-explorer todo)

             (button {:class   "destroy"
                      :onclick #(td-delete! todo)}))

           (letfn [(todo-edt [event]
                     (todo-edit event todo-li))]
             (input {:class     "edit"
                     :onblur    todo-edt
                     :onkeydown todo-edt}))]))))

(defn todo-edit [e todo-li]
  (let [edt-dom (.-target e)
        todo (<mget todo-li :todo)
        li-dom (tag-dom todo-li)]

    (when (classlist/contains li-dom "editing")
      (let [title (str/trim (form/getValue edt-dom))
            stop-editing #(mset!> todo-li :editing? false)]
        (cond
          (or (= (.-type e) "blur")
              (= (.-key e) "Enter"))
          (do
            (stop-editing)                                  ;; has to go first cuz a blur event will sneak in
            (if (= title "")
              (td-delete! todo)
              (mset!> todo :title title)))

          (= (.-key e) "Escape")
          ;; this could leave the input field with mid-edit garbage, but
          ;; that gets initialized correctly when starting editing
          (stop-editing))))))

;;; --- due-by input -------------------------------------------

(defn due-by-input [todo]
  (input {:class     "due-by"
          ::tag/type "date"
          :value     (c?n (when-let [db (td-due-by todo)]
                            (let [db$ (tmc/to-string (tmc/from-long db))]
                              (subs db$ 0 10))))
          :oninput   #(mset!> todo :due-by
                              (tmc/to-long
                                (tmc/from-string
                                  (form/getValue (.-target %)))))
          :style     (c?once (make-css-inline me
                               :border "none"
                               :font-size "14px"
                               :background-color (c? (when-let [clock (mxu-find-class (:tag @me) "std-clock")]
                                                       (if-let [due (td-due-by todo)]
                                                         (if (td-completed todo)
                                                           cache
                                                           (let [time-left (- due (<mget clock :clock))]
                                                             (cond
                                                               (neg? time-left) "red"
                                                               (< time-left (* 24 3600 1000)) "coral"
                                                               (< time-left (* 2 24 3600 1000)) "yellow"
                                                               :default "green")))
                                                         "#e8e8e8")))))}))

;;; --- adverse events ------------------------------------------------------------

(defn de-whitespace [s]
  ($/replace s #"\s" ""))

#_(remove #{\n \r} "https://rxnav.nlm.nih.gov/REST/interaction/list.json?
                   rxcuis=861226+1170673+1151366+316051+1738581+315971+854873+901803")

(def ae-by-brand "https://api.fda.gov/drug/event.json?search=patient.drug.openfda.brand_name:~(~a~)&limit=3")

(defn ae-explorer [todo]
  (button {:class   "li-show"
           :style   (c? (or (when-let [xhr (<mget me :ae)]
                              (let [aes (xhr-response xhr)]
                                (when (= 200 (:status aes))
                                  "display:block")))
                            "display:none"))
           :onclick #(js/alert "Feature not yet implemented.")}

          {:ae (c?+ [:obs (fn-obs
                            (when-not (or (= old unbound) (nil? old))
                              (not-to-be old)))]
                 (when (<mget (mxu-find-class me "ae-autocheck") :on?)
                   (send-xhr (pp/cl-format nil ae-by-brand
                                           (js/encodeURIComponent (td-title todo))))))}

          (span {:style "font-size:0.7em;margin:2px;margin-top:0;vertical-align:top"}
                "View Adverse Events")))

