(ns todomx.core
  (:require
    [cljs.pprint :refer [pprint cl-format]]
    [clojure.browser.repl :as repl]
    [goog.dom :as dom]
    [tiltontec.model.core :refer [<mget]]
    [tiltontec.tag.html :refer [tag-dom-create *tag-trace*]]
    [todomx.build :as tmx]
    [taoensso.tufte :as tufte :refer (defnp p profiled profile)]
    [cljs-time.coerce :refer [from-long to-string] :as tmc]

    [clojurenyc.nyc20180117 :as nyc]))

;; (defonce conn
;;   (repl/connect "http://localhost:9000/repl"))

(enable-console-print!)

(tufte/add-basic-println-handler! {})


(let [root (dom/getElement "tagroot")
      app-matrix
      ;(nyc/hello-world)
      ;(nyc/hello-cells)
      ;(nyc/lifting-clock)
      ;(nyc/lifting-storage)
      ;(nyc/lifting-fda)
      ;(nyc/lifting-html)
      (tmx/matrix-build!)
      app-dom (tag-dom-create
                (<mget app-matrix :mx-dom))]

  (set! (.-innerHTML root) nil)
  (dom/appendChild root app-dom)
  (when-let [route-starter (<mget app-matrix :router-starter)]
    (route-starter)))

