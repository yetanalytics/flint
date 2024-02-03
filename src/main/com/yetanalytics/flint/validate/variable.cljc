(ns com.yetanalytics.flint.validate.variable
  (:require [com.yetanalytics.flint.util :as u]
            [com.yetanalytics.flint.spec.expr :as es]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Expression variables
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti get-expr-vars
  "Return all the variables in an expression."
  (fn [[k _]] k))

(defmethod get-expr-vars :default [_] nil)

(defmethod get-expr-vars :expr/branch [[_ expr]]
  (mapcat get-expr-vars expr))

(defmethod get-expr-vars :expr/args [[_ args]]
  (mapcat get-expr-vars args))

(defmethod get-expr-vars :expr/terminal [[_ expr-term]]
  (get-expr-vars expr-term))

(defmethod get-expr-vars :ax/var [[_ v]] [v])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; SELECT aggregate variables
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti invalid-agg-expr-vars
  "Return a coll of invalid aggregate variables in an expression."
  (fn [_valid-vars [k _]] k))

(defmethod invalid-agg-expr-vars :default [_ _] [])

(defmethod invalid-agg-expr-vars :expr/branch [valid-vars [_ [op-kv args-kv]]]
  (let [[_ op]   op-kv
        [_ args] args-kv]
    (if (or (es/aggregate-ops op)
            (not (symbol? op)))
      []
      (mapcat (partial invalid-agg-expr-vars valid-vars) args))))

(defmethod invalid-agg-expr-vars :expr/terminal [valid-vars [_ x]]
  (invalid-agg-expr-vars valid-vars x))

(defmethod invalid-agg-expr-vars :ax/var [valid-vars [_ v]]
  (if-not (valid-vars v) [v] []))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; GROUP BY projection variables
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti group-by-projected-vars
  "Return a coll of vars that are projected in a GROUP BY clause (i.e.
   all vars not contained within an expression.)"
  (fn [[k _]] k))

(defmethod group-by-projected-vars :group-by [[_ group-by-coll]]
  (->> group-by-coll
       (map group-by-projected-vars)
       (filter some?)))

(defmethod group-by-projected-vars :mod/group-expr [_] nil)

(defmethod group-by-projected-vars :ax/var [[_ v]] v)

(defmethod group-by-projected-vars :mod/expr-as-var [[_ expr-as-var]]
  (-> expr-as-var second second second))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Variable Scopes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti get-scope-vars
  "Return a coll of all variables in the scope of the AST branch."
  (fn [x] (if-some [k (u/get-keyword x)] k :default)))

(defmethod get-scope-vars :default [_] nil)

(defmethod get-scope-vars :ax/var [[_ v]] [v])

(defmethod get-scope-vars :expr/as-var [[_ [_expr v]]]
  (get-scope-vars v))

;; SELECT in-scope vars

(defmethod get-scope-vars :select/expr-as-var [[_ expr-as-var]]
  (get-scope-vars expr-as-var))

;; WHERE in-scope vars

(defmethod get-scope-vars :where [[_ vs]]
  (get-scope-vars vs))

;; Basic Graph Pattern

(defmethod get-scope-vars :triple/object [[_ object]]
  (get-scope-vars object))

(defmethod get-scope-vars :triple/list [[_ list]]
  (mapcat get-scope-vars list))

(defmethod get-scope-vars :triple/vec [[_ spo]]
  (mapcat get-scope-vars spo))

(defmethod get-scope-vars :triple/o [[_ o]]
  (mapcat get-scope-vars o))

(defmethod get-scope-vars :triple/po [[_ po]]
  (reduce-kv (fn [acc p o] (apply concat
                                  acc
                                  (get-scope-vars p)
                                  (map get-scope-vars o)))
             []
             po))

(defmethod get-scope-vars :triple/spo [[_ spo]]
  (reduce-kv (fn [acc s po] (apply concat
                                   acc
                                   (get-scope-vars s)
                                   (map get-scope-vars po)))
             []
             spo))

(defmethod get-scope-vars :triple/spo-list [[_ spo]]
  (reduce-kv (fn [acc s po] (apply concat
                                   acc
                                   (mapcat get-scope-vars s)
                                   (map get-scope-vars po)))
             []
             spo))

(defmethod get-scope-vars :triple/nform [[_ nform]]
  (get-scope-vars nform))

;; Path

(defmethod get-scope-vars :triple/path [[_ p]]
  (get-scope-vars p))

(defmethod get-scope-vars :path/branch [[_ [_op [_k paths]]]]
  (mapcat get-scope-vars paths))

(defmethod get-scope-vars :path/terminal [[_ v]]
  (get-scope-vars v))

;; Group

(defmethod get-scope-vars :where/triple [[_ vs]]
  (get-scope-vars vs))

(defmethod get-scope-vars :where/special [[_ vs]]
  (get-scope-vars vs))

(defmethod get-scope-vars :where/recurse [[_ vs]]
  (get-scope-vars vs))

(defmethod get-scope-vars :select/expr-as-var [[_ ev]]
  (get-scope-vars ev))

(defmethod get-scope-vars :where-sub/select [[_ s]]
  (let [[_ select] (u/get-kv-pair s :select)
        where      (u/get-kv-pair s :where)
        ?group-by  (u/get-kv-pair s :group-by)]
    (case (first select)
      :ax/wildcard
      (cond-> (get-scope-vars where)
        ?group-by ; Should never happen in a legal SELECT query
        (concat (group-by-projected-vars ?group-by)))
      :select/var-or-exprs
      (mapcat get-scope-vars (second select)))))

(defmethod get-scope-vars :where-sub/where [[_ vs]]
  (mapcat get-scope-vars vs))

(defmethod get-scope-vars :where-sub/empty [_] [])

;; WHERE modifiers

(defmethod get-scope-vars :where/union [[_ vs]]
  (mapcat get-scope-vars vs))

(defmethod get-scope-vars :where/optional [[_ vs]]
  (get-scope-vars vs))

(defmethod get-scope-vars :where/graph [[_ [term vs]]]
  (concat (get-scope-vars term) (get-scope-vars vs)))

(defmethod get-scope-vars :where/service [[_ [term vs]]]
  (concat (get-scope-vars term) (get-scope-vars vs)))

(defmethod get-scope-vars :where/service-silent [[_ [term vs]]]
  (concat (get-scope-vars term) (get-scope-vars vs)))

(defmethod get-scope-vars :where/bind [[_ vs]]
  (get-scope-vars vs))

(defmethod get-scope-vars :where/values [[_ [_ values]]]
  (mapcat get-scope-vars (first values)))
