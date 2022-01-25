(ns syrup.sparql.format.update
  (:require [clojure.string :as cstr]
            [syrup.sparql.format :as f]
            [syrup.sparql.format.axiom]
            [syrup.sparql.format.prologue]
            [syrup.sparql.format.triple]
            [syrup.sparql.format.where]))

(defn- format-quads [quads]
  (str "{\n" (f/indent-str (cstr/join "\n" quads)) "\n}"))

(defmethod f/format-ast :update/kw [[_ kw]]
  (case kw
    :default "DEFAULT"
    :named   "NAMED"
    :all     "ALL"))

(defmethod f/format-ast :update/iri [[_ iri]]
  iri)

(defmethod f/format-ast :update/named-iri [[_ [_ iri]]]
  (str "NAMED " iri))

(defmethod f/format-ast :update/default-graph [_]
  "DEFAULT")

(defmethod f/format-ast :update/named-graph [[_ iri]]
  (str "GRAPH " iri))

(defmethod f/format-ast :quads [[_ [_ var-or-iri triples]]]
  (str "GRAPH " var-or-iri " " (format-quads triples)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Graph Management specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod f/format-ast :into [[_ in]]
  (str "INTO " in))

(defmethod f/format-ast :to [[_ to]]
  (str "TO " to))

(defmethod f/format-ast :load [[_ ld]]
  (str "LOAD " ld))

(defmethod f/format-ast :load-silent [[_ ld-silent]]
  (str "LOAD SILENT " ld-silent))

(defmethod f/format-ast :clear [[_ clr]]
  (str "CLEAR " clr))

(defmethod f/format-ast :clear-silent [[_ clr-silent]]
  (str "CLEAR SILENT " clr-silent))

(defmethod f/format-ast :drop [[_ drp]]
  (str "DROP " drp))

(defmethod f/format-ast :drop-silent [[_ drp-silent]]
  (str "DROP SILENT " drp-silent))

(defmethod f/format-ast :create [[_ create]]
  (str "CREATE " create))

(defmethod f/format-ast :create-silent [[_ create-silent]]
  (str "CREATE SILENT " create-silent))

(defmethod f/format-ast :add [[_ add]]
  (str "ADD " add))

(defmethod f/format-ast :add-silent [[_ add-silent]]
  (str "ADD SILENT " add-silent))

(defmethod f/format-ast :move [[_ move]]
  (str "MOVE " move))

(defmethod f/format-ast :move-silent [[_ move-silent]]
  (str "MOVE SILENT " move-silent))

(defmethod f/format-ast :copy [[_ copy]]
  (str "COPY " copy))

(defmethod f/format-ast :copy-silent [[_ copy-silent]]
  (str "COPY SILENT " copy-silent))

(defmethod f/format-ast :load-update [[_ load-update]]
  (cstr/join "\n" load-update))

(defmethod f/format-ast :clear-update [[_ clear-update]]
  (cstr/join "\n" clear-update))

(defmethod f/format-ast :drop-update [[_ drop-update]]
  (cstr/join "\n" drop-update))

(defmethod f/format-ast :create-update [[_ create-update]]
  (cstr/join "\n" create-update))

(defmethod f/format-ast :add-update [[_ add-update]]
  (cstr/join "\n" add-update))

(defmethod f/format-ast :move-update [[_ move-update]]
  (cstr/join "\n" move-update))

(defmethod f/format-ast :copy-update [[_ copy-update]]
  (cstr/join "\n" copy-update))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Graph Management specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod f/format-ast :using [[_ using]]
  (str "USING " using))

(defmethod f/format-ast :with [[_ with]]
  (str "WITH " with))

(defmethod f/format-ast :insert-data [[_ insert-data]]
  (str "INSERT DATA " (format-quads insert-data)))

(defmethod f/format-ast :delete-data [[_ delete-data]]
  (str "DELETE DATA " (format-quads delete-data)))

(defmethod f/format-ast :delete-where [[_ delete-where]]
  (str "DELETE WHERE " (format-quads delete-where)))

(defmethod f/format-ast :delete [[_ delete]]
  (str "DELETE " (format-quads delete)))

(defmethod f/format-ast :insert [[_ insert]]
  (str "INSERT " (format-quads insert)))

(defmethod f/format-ast :insert-data-update [[_ id-update]]
  (cstr/join "\n" id-update))

(defmethod f/format-ast :delete-data-update [[_ dd-update]]
  (cstr/join "\n" dd-update))

(defmethod f/format-ast :delete-where-update [[_ dw-update]]
  (cstr/join "\n" dw-update))

(defmethod f/format-ast :modify-update [[_ mod-update]]
  (cstr/join "\n" mod-update))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Updates
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod f/format-ast :update-request [[_ updates]]
  (cstr/join ";\n" updates))
