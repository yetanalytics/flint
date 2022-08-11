(ns com.yetanalytics.flint.axiom.impl.validation)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Regex
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; 

(def iri-regex*
  #"([^<>{}\"\\|\^`\s])*")

(def iri-regex
  #"<([^<>{}\"\\|\^`\s])*>")

(def prefix-iri-ns-regex
  #"[A-Za-z_]([\w\-\.]*[\w\-])?")

(def prefix-iri-name-regex
  #"[\w\-]+")

(def variable-regex
  #"\?\w+")

(def bnode-regex
  #"_(\w([\w\.]*\w)?)?")

;; Note in the second part that we need to match the letters `n`, `r`, etc.,
;; not the newline char `\n` nor the return char `\r`.
(def valid-str-regex
  #"([^\"\r\n\\]|(?:\\(?:t|b|n|r|f|\\|\"|')))*")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Validation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; IRIs and RDF terms

(defn valid-iri-string?*
  "Is `x` an IRI string? Note that `x` can be an otherwise non-IRI
   (e.g. `<foo>`)."
  [s]
  (boolean (re-matches iri-regex* s)))

(defn valid-iri-string?
  "Is `x` a wrapped (i.e. starts with `<` and ends with `>`) IRI?
   Note that `x` can be an otherwise non-IRI (e.g. `<foo>`)."
  [s]
  (boolean (re-matches iri-regex s)))

(defn valid-prefix-keyword?
  "Is `k` a valid SPARQL prefix keyword?"
  [k]
  (boolean (and (nil? (namespace k))
                (or (re-matches prefix-iri-ns-regex (name k))
                    (= :$ k)))))

(defn valid-prefix-iri-keyword?
  "Is `k` a potentially namespaced keyword?"
  [k]
  (let [kns   (namespace k)
        kname (name k)]
    (boolean (and (not (#{:a :*} k))
                  (or (nil? kns)
                      (re-matches prefix-iri-ns-regex kns))
                  (re-matches prefix-iri-name-regex kname)))))

(defn valid-var-symbol?
  "Is `var-sym` a symbol that starts with `?`?"
  [var-sym]
  (boolean (re-matches variable-regex (name var-sym))))

(defn valid-bnode-symbol?
  "Is `bnode-sym` a symbol that starts with `_` and has zero or more
   trailing chars?"
  [bnode-sym]
  (boolean (re-matches bnode-regex (name bnode-sym))))

;; Literals

(defn valid-string-literal?
  "Is `str-lit` a string and does not contains unescaped `\"`, `\\`, `\\n`,
   nor `\\r`? (This filtering is to avoid SPARQL injection attacks.)"
  [str-lit]
  (boolean (re-matches valid-str-regex str-lit)))

(defn valid-lang-map-literal?
  "Is `lang-map` a singleton map between a language tag and valid string?"
  [lang-map]
  (boolean (and (->> lang-map count (= 1))
                (->> lang-map keys first keyword?)
                (->> lang-map vals first (re-matches valid-str-regex)))))
