(ns todomx.gettingstarted
  (:require [cljs.pprint :as pp]
            [cljs-time.coerce :as tmc]
            [clojure.string :as str]
            [bide.core :as r]
            [taoensso.tufte :as tufte :refer-macros (defnp profiled profile)]

            [tiltontec.util.core :refer [pln xor now]]
            [tiltontec.cell.base :refer [unbound ia-type *within-integrity* *defer-changes*]]
            [tiltontec.cell.core
             :refer-macros [c? c?+ c?n c?+n c?once]
             :refer [c-in c-reset!]]
            [tiltontec.cell.observer :refer-macros [fn-obs]]
            [tiltontec.cell.evaluate :refer [c-awaken <cget]]
            [tiltontec.cell.core
             :refer-macros [c? c?+ c-reset-next! c?once c?n]
             :refer [c-in cset!> make-cell make-c-formula]]


            [tiltontec.model.core
             :refer-macros [with-par]
             :refer [matrix mx-par <mget mset!> mswap!>
                     fget mxi-find mxu-find-type
                     kid-values-kids] :as md]
            [tiltontec.tag.html
             :refer [io-read io-upsert io-clear-storage
                     tag-dom-create
                     dom-tag tagfo tag-dom
                     dom-has-class dom-ancestor-by-tag]
             :as tag]

            [tiltontec.tag.gen
             :refer-macros [section header h1 input footer p a span label ul li div button br]
             :refer [dom-tag evt-tag]]

            [tiltontec.tag.style :refer [make-css-inline]]))

(defn hello-world []
      ; A true hello-world, with just enough code to let us
      ; know we can execute our tool chain from begiining to end.
      ;
      ; Our core driver expects (1) an atom (2) containing a so-called matrix (a map)
      ; with (3) a property :mx-dom indexing something suitable for consumption by
      ; the Tag web framework we will be looking at later.
      ;
      ; (Please do not worry about the web aspect just yet; at first the console
      ; will show the important action.)
      ;
      ; We'll just return a normal atom and map and throughly unimpressive DOM and
      ; make sure this much works.
      ;
      ; What you should do:
      ; 1. In a terminal, cd to the project root directory.
      ; 2. Enter: scripts/build
      ; 3. Confirm no errors from the build. Or just go for it:
      ; 4. Open index.html (also in the root directory) in an HTML5-ready browser.
      ; 5. Open your browser's developer console.
      ;
      ; What you should see:
      ; 1. "hello, world" in the browser page.
      ; 2. "hello, console" in the console.
      ;


      (println "hello, console")

      (atom {:mx-dom [(h1 "hello, world") (br)]}))

(defn input-cells
      "   Great. Now let's create a boring 'input' cell. It cannot
     be formulas all the way down, and input cells are where
     our models get fed un-computed data.

     No formula yet to *use* that input, but we can demonstrate
     a still boring bit of life with a so-called observer dumping
     some info to the console.

     What you should do:
     1. Modify the app-matrix binding in todomx.core to pick up (start/input-cells)
     2. Reload the browser

     What you should see:
     1. Confirmation in the browser page that we are looking at this exaample.
     2. In the console: 'Billie Jean' changing her name to 'Marilyn'."
        []


      (let [name (c-in "Billie Jean"
                       :obs (fn [me slot new old c]
                                (println :observing-name-new new :old-was old)))]
           (cset!> name "Marilyn"))

      (atom {:mx-dom [(h1 "input-cells, observed")
                      (p "Console should show name being observed.")]}))

(defn formula-one
      "Enough baby steps. Let's get reactive.

      What you should do:
      1. Modify the app-matrix binding in todomx.core to pick up (start/formula-one)
      2. Reload the browser.

      What you should see:
      1. Confirmation in the browser page that we are looking at this exaample.
      2. In the console:
       - after the new name is announced, just one println indicating message is computing
       - one message composed of the new name and its length.

      Takeaways:
      0. It's alive! We have dataflow. Some say 'reactive'. That sounds like 'pull', we see change pushing.
      1. Just one computation, though new data reached the message from two paths.
      2. No new computation when assignment does not in fact change anything.
      2. No explicit publish or subscribe, just read and assign using standard matrix getters
      "
      []

      (let [name (c-in "Billie Jean"
                       :obs (fn [me slot new old c]
                                (println :observing-name-new new :old-was old)))
            name-len (c? (count (<cget name)))
            message (c?+ [:slot :message
                          :obs (fn-obs (println :new-message! new))]
                         (println :computing-message)
                         (str "The actress's name is now " (<cget name) " of length " (<cget name-len)))]
           (cset!> name "Marilyn")
           (cset!> name "Marilyn")) ;; no propagation off assigning same value

      (atom {:mx-dom [(h1 "formula-one (and two), observed")
                      (p "Console should show message (in one computation) being observed.")]}))

(def ae-by-brand "https://api.fda.gov/drug/event.json?search=patient.drug.openfda.brand_name:~a&limit=3")

(defn talk-to-your-doctor
      "Let us bring an adverse event REST database into the Matrix. FDA.gov, to be specific.
      We will need the Matrix-aware XHR module so our asynchronous request feeds
      gracefully into the dataflow when it eventually responds. (We fill fake a delay to
      make that discernible.

      We will also graduate from standalone Cells to a Model with individual
      properties as Cells, the normal way of building a matrix.

      What you should do:
      1. Modify the app-matrix binding in todomx.core to pick up (start/talk-to-your-doctor)
      2. Reload the browser.

      What you should see:
      1. Confirmation in the browser page that we are looking at this exaample.
      2. In the console:
       - after the new name is announced, just one println indicating message is computing
       - one message composed of the new name and its length.

      Takeaways:
      0. It's alive! We have dataflow. Some say 'reactive'. That sounds like 'pull', we see change pushing.
      1. Just one computation, though new data reached the message from two paths.
      2. No new computation when assignment does not in fact change anything.
      2. No explicit publish or subscribe, just read and assign using standard matrix getters
      "
      []

      (md/make ::fda-gov
        ;; load all to-dos into a depend-able list....
        :drug (c-in nil)
        :ae nil #_(c? (when-let [drug (<mget me :drug)]
                                (response (send-xhr :brand-adv-events
                                                    (cl-format nil ae-by-brand drug)))))

        ;; build the matrix dom once. From here on, all DOM changes are
        ;; made incrementally by Tag library observers...
        :mx-dom [(h1 "Talk to your doctor")
                 (p "Console TBD")]))
