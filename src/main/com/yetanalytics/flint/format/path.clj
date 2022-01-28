(ns com.yetanalytics.flint.format.path
  (:require [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.axiom]))

(defmethod f/format-ast :path/op [_ [_ op]] (keyword op))

(defmethod f/format-ast :path/args [_ [_ args]] args)

(defn- parens-if-nests
  "Super-basic precedence comparison to wrap parens if there's an inner
   unary regex op, since paths like `a?*+` are illegal."
  [arg]
  (if (re-matches #".*(\?|\*|\+)" arg)
    (str "(" arg ")")
    arg))

(defmethod f/format-ast :path/branch [_ [_ [op args]]]
  (case op
    :alt (str "(" (cstr/join " | " args) ")")
    :cat (str "(" (cstr/join " / " args) ")")
    :inv (str "^" (first args))
    :?   (-> args first parens-if-nests (str "?"))
    :*   (-> args first parens-if-nests (str "*"))
    :+   (-> args first parens-if-nests (str "+"))
    :not (str "!" (cstr/join " | " args))))

(defmethod f/format-ast :path/terminal [_ [_ value]]
  value)
