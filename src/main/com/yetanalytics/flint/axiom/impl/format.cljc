(ns com.yetanalytics.flint.axiom.impl.format)

;; IRIs and RDF terms

(defn format-prefix-iri-keyword [k]
  (let [kns   (namespace k)
        kname (name k)]
    (str kns ":" kname)))

(defn format-var-symbol [v-sym]
  (name v-sym))

(defn format-bnode-symbol [b-sym]
  (if-some [?suffix (second (re-matches #"_(.+)" (name b-sym)))]
    (str "_:" ?suffix)
    "[]"))

;; Literals

(defn format-string-literal [str-lit]
  (str "\"" str-lit "\""))

(defn format-lang-map-tag [lang-map]
  (-> lang-map keys first name))

(defn format-lang-map-val [lang-map]
  (-> lang-map vals first))

(defn format-lang-map-literal [lang-map]
  (let [ltag (format-lang-map-tag lang-map)
        lval (format-lang-map-val lang-map)]
    (str "\"" lval "\"@" ltag)))

(def xsd-iri-prefix
  "http://www.w3.org/2001/XMLSchema#")

(defn- get-xsd-prefix*
  [prefixes]
  (reduce-kv (fn [_ k v]
               (if (= xsd-iri-prefix v)
                 (reduced (name k))
                 nil))
             nil
             prefixes))

(def ^{:private true :arglists '([prefixes])} get-xsd-prefix
  (memoize get-xsd-prefix*))

(defn format-xsd-typed-literal
  ([lit-val xsd-suffix]
   (format-xsd-typed-literal lit-val xsd-suffix nil))
  ([lit-val xsd-suffix prefixes]
   (if-some [xsd-prefix (get-xsd-prefix prefixes)]
     (str "\"" lit-val "\"^^" xsd-prefix ":" xsd-suffix)
     (str "\"" lit-val "\"^^" xsd-iri-prefix xsd-suffix))))
