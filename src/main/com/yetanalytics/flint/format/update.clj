(ns com.yetanalytics.flint.format.update
  (:require [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.axiom]
            [com.yetanalytics.flint.format.prologue]
            [com.yetanalytics.flint.format.triple]
            [com.yetanalytics.flint.format.where]))

(defn- format-quads [quads]
  (str "{\n" (f/indent-str (cstr/join "\n" quads)) "\n}"))

(defmethod f/format-ast :update/kw [_ [_ kw]]
  (case kw
    :default "DEFAULT"
    :named   "NAMED"
    :all     "ALL"))

(defmethod f/format-ast :update/iri [_ [_ iri]]
  iri)

(defmethod f/format-ast :update/named-iri [_ [_ [_ iri]]]
  (str "NAMED " iri))

(defmethod f/format-ast :update/default-graph [_ _]
  "DEFAULT")

(defmethod f/format-ast :update/named-graph [_ [_ iri]]
  iri)

(defmethod f/format-ast :triple/quads [_ [_ [_ var-or-iri triples]]]
  (str "GRAPH " var-or-iri " " (format-quads triples)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Graph Management specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod f/format-ast :into [_ [_ in]]
  (str "INTO " in))

(defmethod f/format-ast :to [_ [_ to]]
  (str "TO " to))

(defmethod f/format-ast :load [_ [_ ld]]
  (str "LOAD " ld))

(defmethod f/format-ast :load-silent [_ [_ ld-silent]]
  (str "LOAD SILENT " ld-silent))

(defmethod f/format-ast :clear [_ [_ clr]]
  (str "CLEAR " clr))

(defmethod f/format-ast :clear-silent [_ [_ clr-silent]]
  (str "CLEAR SILENT " clr-silent))

(defmethod f/format-ast :drop [_ [_ drp]]
  (str "DROP " drp))

(defmethod f/format-ast :drop-silent [_ [_ drp-silent]]
  (str "DROP SILENT " drp-silent))

(defmethod f/format-ast :create [_ [_ create]]
  (str "CREATE " create))

(defmethod f/format-ast :create-silent [_ [_ create-silent]]
  (str "CREATE SILENT " create-silent))

(defmethod f/format-ast :add [_ [_ add]]
  (str "ADD " add))

(defmethod f/format-ast :add-silent [_ [_ add-silent]]
  (str "ADD SILENT " add-silent))

(defmethod f/format-ast :move [_ [_ move]]
  (str "MOVE " move))

(defmethod f/format-ast :move-silent [_ [_ move-silent]]
  (str "MOVE SILENT " move-silent))

(defmethod f/format-ast :copy [_ [_ copy]]
  (str "COPY " copy))

(defmethod f/format-ast :copy-silent [_ [_ copy-silent]]
  (str "COPY SILENT " copy-silent))

(defmethod f/format-ast :load-update [_ [_ load-update]]
  (cstr/join "\n" load-update))

(defmethod f/format-ast :clear-update [_ [_ clear-update]]
  (cstr/join "\n" clear-update))

(defmethod f/format-ast :drop-update [_ [_ drop-update]]
  (cstr/join "\n" drop-update))

(defmethod f/format-ast :create-update [_ [_ create-update]]
  (cstr/join "\n" create-update))

(defmethod f/format-ast :add-update [_ [_ add-update]]
  (cstr/join "\n" add-update))

(defmethod f/format-ast :move-update [_ [_ move-update]]
  (cstr/join "\n" move-update))

(defmethod f/format-ast :copy-update [_ [_ copy-update]]
  (cstr/join "\n" copy-update))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Graph Management specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod f/format-ast :using [_ [_ using]]
  (str "USING " using))

(defmethod f/format-ast :with [_ [_ with]]
  (str "WITH " with))

(defmethod f/format-ast :insert-data [_ [_ insert-data]]
  (str "INSERT DATA " (format-quads insert-data)))

(defmethod f/format-ast :delete-data [_ [_ delete-data]]
  (str "DELETE DATA " (format-quads delete-data)))

(defmethod f/format-ast :delete-where [_ [_ delete-where]]
  (str "DELETE WHERE " (format-quads delete-where)))

(defmethod f/format-ast :delete [_ [_ delete]]
  (str "DELETE " (format-quads delete)))

(defmethod f/format-ast :insert [_ [_ insert]]
  (str "INSERT " (format-quads insert)))

(defmethod f/format-ast :insert-data-update [_ [_ id-update]]
  (cstr/join "\n" id-update))

(defmethod f/format-ast :delete-data-update [_ [_ dd-update]]
  (cstr/join "\n" dd-update))

(defmethod f/format-ast :delete-where-update [_ [_ dw-update]]
  (cstr/join "\n" dw-update))

(defmethod f/format-ast :modify-update [_ [_ mod-update]]
  (cstr/join "\n" mod-update))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Updates
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod f/format-ast :update-request [_ [_ updates]]
  (cstr/join ";\n" updates))
