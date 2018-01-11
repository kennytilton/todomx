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
             :refer-macros [section header h1 input footer p a span label ul li div button br]
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
                     td-id td-toggle-completed!]]
            [cljs-time.coerce :as tmc]
            [clojure.string :as $]))

(declare todo-edit )

(defn todo-list-item [me todo matrix]
  (println :building-li (:title @todo))
  (li {:class   (c? [(when (<mget me :selected?) "chosen")
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

       :selected? (c? (some #{todo} (<mget (mxu-find-tag me :ul) :selections)))

       :editing   (c-in false)}

      (div {:class "view"}
        (input {:class   "toggle" ::tag/type "checkbox"
                :checked (c? (not (nil? (td-completed todo))))
                :onclick #(td-toggle-completed! todo)})

        (label {:onclick   (fn [evt]
                             (mswap!> (mxu-find-tag me :ul) :selections
                                      #(if (some #{todo} %)
                                          (remove #{todo} %)
                                          (conj % todo))))

                :ondblclick #(let [li-dom (dom/getAncestorByTagNameAndClass
                                            (.-target %) "li")
                                   edt-dom (dom/getElementByClass
                                             "edit" li-dom)]
                               (classlist/add li-dom "editing")
                               (tag/input-editing-start edt-dom (td-title todo)))}
               (td-title todo))

        (button {:class   "destroy"
                 :onclick #(td-delete! todo)}))

      (input {:class     "edit"
              :onblur    #(todo-edit % todo)
              :onkeydown #(todo-edit % todo)})))


(defn todo-edit [e todo]
  (let [edt-dom (.-target e)
        li-dom (dom/getAncestorByTagNameAndClass edt-dom "li")]

    (when (classlist/contains li-dom "editing")
      (let [title (str/trim (form/getValue edt-dom))
            stop-editing #(classlist/remove li-dom "editing")]
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