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

(defmethod f/format-ast :path-branch [[_ {:keys [op paths path-precs]}]]
  (let [op-prec   (get-precedence op)
        path-strs (map (fn [path path-prec]
                         (if (> op-prec path-prec)
                           (str "(" path ")")
                           path))
                       paths
                       path-precs)]
    (case (keyword op)
      :alt (cstr/join " | " path-strs)
      :cat (cstr/join " / " path-strs)
      :inv (str "^" (first path-strs))
      :?   (str (first path-strs) "?")
      :*   (str (first path-strs) "*")
      :+   (str (first path-strs) "+")
      :not (str "!" (first path-strs)))))

(defmethod f/format-ast :path-terminal [[_ value]]
  value)

(defmethod f/annotate-ast :path-branch [[kw {:keys [paths] :as branch}]]
  (let [path-precs (map (fn [path]
                          (if (and (vector? path)
                                   (= :path-branch (first path)))
                            (get-precedence (-> path second :op))
                            max-precedence))
                        paths)]
    [kw (assoc branch :path-precs path-precs)]))
