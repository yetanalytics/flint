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

(defn- semicolon-sep?
  [op]
  (#{'group-concat 'group-concat-distinct} op))

(defn- graph-pat-exp?
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
      "encode-for-uri"        "ENCODE_FOR_URI"
      "group-concat"          "GROUP_CONCAT"
      "group-concat-distinct" "GROUP_CONCAT"
      "not-exists"            "NOT EXISTS"
      (if-not (or (cstr/includes? op-name "<")
                  (cstr/includes? op-name ":"))
        (if-some [pred-prefix (second (re-matches #"(.+)\?" op-name))]
          (str "is" (cstr/upper-case pred-prefix))
          (cstr/upper-case (cstr/replace op-name #"-" "")))
        op-name))))

(defn- distinct-op?
  [op]
  (re-matches #"(.+)-distinct" (name op)))

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

(defmethod f/format-ast-node :expr/branch [_ [_ [op args]]]
  (let [op-str (op->str op)
        ?dist  (when (distinct-op? op) "DISTINCT ")]
    (cond
      (elist-op? op) (str "(" (first args) " " op-str " (" (cstr/join ", " (rest args)) "))")
      (infix-op? op) (str "(" (cstr/join (str " " op-str " ") args) ")")
      (unary-op? op) (str op-str (-> args first parens-if-nests))
      (semicolon-sep? op) (str op-str "(" ?dist (cstr/join "; " args) ")")
      (graph-pat-exp? op) (str op-str " " (first args))
      :else (str op-str "(" ?dist (cstr/join ", " args) ")"))))

(defmethod f/format-ast-node :expr/terminal [_ [_ terminal]]
  terminal)

(defmethod f/format-ast-node :expr/as-var [_ [_ [expr var]]]
  (str expr " AS " var))
