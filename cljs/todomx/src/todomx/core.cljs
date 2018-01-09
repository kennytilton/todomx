(ns todomx.core
  (:require
    [clojure.browser.repl :as repl]
    [goog.dom :as dom]
    [tiltontec.model.core :as md]
    [tiltontec.tag.html :refer [tag-dom-create *tag-trace*]]
    [todomx.todomx :as tmx]
    [taoensso.tufte :as tufte :refer (defnp p profiled profile)]
    [cljs-time.coerce :refer [from-long to-string] :as tmc]))

;; (defonce conn
;;   (repl/connect "http://localhost:9000/repl"))

(enable-console-print!)

(tufte/add-basic-println-handler! {})

(let [root (dom/getElement "tagroot")
      app-matrix (tmx/matrix-build!)
      app-dom (binding [*tag-trace* nil]                ;; <-- set to nil if console too noisy
                (tag-dom-create
                  (md/md-get app-matrix :mx-dom)))]

  (set! (.-innerHTML root) nil)
  (dom/appendChild root app-dom)
  (when-let [route-starter (md/md-get app-matrix :router-starter)]
    (route-starter)))

