(ns com.yetanalytics.flint.format.expr
  (:require [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.axiom]))

;; NOTE: Any deps that call this namespace should also call the format.where
;; ns, to properly format EXISTS and NOT EXISTS.
;; format.where cannot directly be called here since it would cause a cyclic
;; dependency.

(defn- elist-op?
  [op]
  (#{'in 'not-in} op))

(defn- infix-op?
  [op]
  (#{'= 'not= '< '> '<= '>= 'and 'or '+ '- '* '/} op))

(defn- unary-op?
  [op]
  (#{'not} op))

(defn- where-op?
  [op]
  (#{'exists 'not-exists} op))

(defn- op->str
  [op]
  (let [op-name (name op)]
    (case op-name
      "-"      "-" ; Otherwise will be removed below
      "not="   "!="
      "and"    "&&"
      "or"     "||"
      "in"     "IN"
      "not-in" "NOT IN"
      "not"    "!"
      ;; Function names with unique hyphen rules
      "encode-for-uri" "ENCODE_FOR_URI"
      "group-concat"   "GROUP_CONCAT"
      "not-exists"     "NOT EXISTS"
      (if-not (or (cstr/includes? op-name "<")
                  (cstr/includes? op-name ":"))
        ;; Hyphenate and convert `pred?` to `isPred`
        (if-some [pred-prefix (second (re-matches #"(.+)\?" op-name))]
          (str "is" (cstr/upper-case pred-prefix))
          (cstr/upper-case (cstr/replace op-name #"-" "")))
        ;; Custom name
        op-name))))

(defn- parens-if-nests
  "Super-basic precedence comparison to wrap parens if there's an inner
   unary op, since expressions like `!!true` are illegal."
  [arg]
  (if (re-matches #"(\!|\+|\-).*" arg)
    (str "(" arg ")")
    arg))

(defmethod f/format-ast-node :expr/kwarg [_ [_ [[_ k] [_ v]]]]
  (str (cstr/upper-case (name k)) " = '" v "'"))

(defmethod f/format-ast-node :expr/op [_ [_ op]] op)

(defmethod f/format-ast-node :expr/args [_ [_ args]] args)

;; Keywords - don't convert to string

(defmethod f/format-ast-node :expr/kwargs [_ [_ kwargs]]
  (into {} kwargs))

(defmethod f/format-ast-node :distinct? [_ x] x)

(defmethod f/format-ast-node :separator [_ x] x)

(defmethod f/format-ast-node :expr/branch [_ [_ [op args ?kwargs]]]
  (let [op-str (op->str op)]
    (cond
      ?kwargs ; ops with kwargs all have regular fn syntax
      (let [{?dist :distinct?
             ?sep  :separator} ?kwargs
            ?dist-str (when ?dist "DISTINCT ")
            ?sep-str  (when ?sep (str "; SEPARATOR = \"" ?sep "\""))]
        (str op-str "(" ?dist-str (cstr/join ", " args) ?sep-str ")"))
      (elist-op? op) (str "(" (first args) " " op-str " (" (cstr/join ", " (rest args)) "))")
      (infix-op? op) (str "(" (cstr/join (str " " op-str " ") args) ")")
      (unary-op? op) (str op-str (-> args first parens-if-nests))
      (where-op? op) (str op-str " " (first args))
      :else (str op-str "(" (cstr/join ", " args) ")"))))

(defmethod f/format-ast-node :expr/terminal [_ [_ terminal]]
  terminal)

(defmethod f/format-ast-node :expr/as-var [_ [_ [expr var]]]
  (str expr " AS " var))
