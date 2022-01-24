(ns syrup.sparql.format.path
  (:require [clojure.string :as cstr]
            [syrup.sparql.format :as f]
            [syrup.sparql.format.axiom]))

(def max-precedence 10)

(defn get-precedence
  [op]
  (cond
    (#{'alt} op) 1
    (#{'cat} op) 2
    (#{'inv} op) 3
    (#{'? '* '+} op) 4
    (#{'not} op) 5
    :else max-precedence))

(defmethod f/format-ast :path/op [[_ op]] (keyword op))

(defmethod f/format-ast :path/args [[_ args]] args)

(defmethod f/format-ast :path/branch [[_ [op args]]]
  (let [
        ;; op-prec   (get-precedence op)
        ;; path-strs (map (fn [path path-prec]
        ;;                  (if (> op-prec path-prec)
        ;;                    (str "(" path ")")
        ;;                    path))
        ;;                paths
        ;;                path-precs)
        ]
    (case op
      :alt (str "(" (cstr/join " | " args) ")")
      :cat (str "(" (cstr/join " / " args) ")")
      :inv (str "^" (first args))
      :?   (str (first args) "?")
      :*   (str (first args) "*")
      :+   (str (first args) "+")
      :not (str "!" (cstr/join " | " args)))))

(defmethod f/format-ast :path/terminal [[_ value]]
  value)

;; (defmethod f/annotate-ast :path-branch [[kw {:keys [paths] :as branch}]]
;;   (let [path-precs (map (fn [path]
;;                           (if (and (vector? path)
;;                                    (= :path-branch (first path)))
;;                             (get-precedence (-> path second :op))
;;                             max-precedence))
;;                         paths)]
;;     [kw (assoc branch :path-precs path-precs)]))
