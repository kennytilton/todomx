(ns todomx.core
  (:require
    [clojure.browser.repl :as repl]
    [goog.dom :as dom]
    [tiltontec.model.core :refer [<mget]]
    [tiltontec.tag.html :refer [tag-dom-create *tag-trace*]]
    [todomx.build :as tmx]
    [taoensso.tufte :as tufte :refer (defnp p profiled profile)]
    [cljs-time.coerce :refer [from-long to-string] :as tmc]
    [todomx.gettingstarted :as start]))

;; (defonce conn
;;   (repl/connect "http://localhost:9000/repl"))

(enable-console-print!)

(tufte/add-basic-println-handler! {})

(let [root (dom/getElement "tagroot")
      app-matrix (start/formula-one)
      app-dom (tag-dom-create
                (<mget app-matrix :mx-dom))]

  (set! (.-innerHTML root) nil)
  (dom/appendChild root app-dom))

