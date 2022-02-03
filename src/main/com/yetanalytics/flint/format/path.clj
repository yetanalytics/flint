(ns com.yetanalytics.flint.format.path
  (:require [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.axiom]))

(defmethod f/format-ast-node :path/op [_ [_ op]] (keyword op))

(defmethod f/format-ast-node :path/paths [_ [_ paths]] paths)

(defn- parens-if-nests
  "Super-basic precedence comparison to wrap parens if there's an inner
   unary regex op, since paths like `a?*+` are illegal."
  [arg]
  (if (re-matches #".*(\?|\*|\+)" arg)
    (str "(" arg ")")
    arg))

(defmethod f/format-ast-node :path/branch [_ [_ [op paths]]]
  (case op
    :alt (str "(" (cstr/join " | " paths) ")")
    :cat (str "(" (cstr/join " / " paths) ")")
    :inv (str "^" (first paths))
    :?   (-> paths first parens-if-nests (str "?"))
    :*   (-> paths first parens-if-nests (str "*"))
    :+   (-> paths first parens-if-nests (str "+"))
    :not (str "!" (cstr/join " | " paths))))

(defmethod f/format-ast-node :path/terminal [_ [_ value]]
  value)
