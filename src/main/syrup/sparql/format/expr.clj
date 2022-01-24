(ns syrup.sparql.format.expr
  (:require [clojure.string :as cstr]
            [syrup.sparql.format :as f]
            [syrup.sparql.format.axiom]))

(defn- infix-op?
  [op paths]
  (and (#{'= 'not= '< '> '<= '>= 'and 'or 'in 'not-in '+ '- '* '/} op)
       (< 1 (count paths))))

(defn- unary-op?
  [op paths]
  (and (#{'+ '- 'not} op)
       (= 1 (count paths))))

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

(defn- semicolon-sep?
  [op]
  (#{'group-concat 'group-concat-distinct} op))

(def max-precedence 10)

(defn get-precedence
  [op args]
  (cond
    (unary-op? op args) max-precedence
    (#{'or} op) 1
    (#{'and} op) 2
    (#{'= 'not= '< '> '<= '>= 'in 'not-in} op) 3
    (#{'+ '-} op) 4
    (#{'* '/} op) 5
    (#{'not} op) 6
    :else max-precedence))

(defmethod f/format-ast :expr/kwarg [[_ [k v]]]
  (str (cstr/upper-case (name k)) " = '" v "'"))

(defmethod f/format-ast :expr/op [[_ op]] op)

(defmethod f/format-ast :expr/args [[_ args]] args)

(defmethod f/format-ast :expr/branch [[_ [op args]]]
  (let [; op-prec  (get-precedence op args)
        op-str   (op->str op)
        ;; infix?   (infix-op? op args)
        ;; unary?   (unary-op? op args)
        ; prec-cmp (if (and infix? (#{'- '/} op)) >= >)
        ;; arg-strs (if (or (infix-op? op args)
        ;;                  (unary-op? op args))
        ;;            (map (fn [arg-str] (str "(" arg-str ")")) args)
        ;;            #_(map (fn arg->str [arg-str arg-prec]
        ;;                   (if (prec-cmp op-prec arg-prec)
        ;;                     (str "(" arg-str ")")
        ;;                     arg-str))
        ;;                 args
        ;;                 (or arg-precs (repeat (count args) 0)))
        ;;            args)
        ?dist (when (distinct-op? op) "DISTINCT ")]
    (cond
      (infix-op? op args) (str "(" (cstr/join (str " " op-str " ") args) ")")
      (unary-op? op args) (str op-str (first args))
      (semicolon-sep? op) (str op-str "(" ?dist (cstr/join "; " args) ")")
      :else               (str op-str "(" ?dist (cstr/join ", " args) ")"))))

(defmethod f/format-ast :expr/terminal [[_ terminal]]
  terminal)

(defmethod f/format-ast :expr/as-var [[_ [expr var]]]
  (str expr " AS " var))

;; (defmethod f/annotate-ast :expr/branch [[kw [op [_ args]]]]
;;   (let [arg-precs (map (fn [arg]
;;                          (if (and (vector? arg)
;;                                   (= :expr/branch (first arg)))
;;                            (get-precedence (-> arg second :op)
;;                                            (-> arg second :args))
;;                            max-precedence))
;;                        args)]
;;     [kw [op [:expr/args args] arg-precs]]))
