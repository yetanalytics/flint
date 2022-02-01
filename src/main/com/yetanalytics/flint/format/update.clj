(ns com.yetanalytics.flint.format.update
  (:require [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.axiom]
            [com.yetanalytics.flint.format.prologue]
            [com.yetanalytics.flint.format.triple]
            [com.yetanalytics.flint.format.where]))

(defn- format-quads [quads pretty?]
  (if pretty?
    (str "{\n" (f/indent-str (cstr/join "\n" quads)) "\n}")
    (str "{ " (cstr/join " " quads) " }")))

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

(defmethod f/format-ast :triple/quads [{:keys [pretty?]} [_ [_ var-or-iri triples]]]
  (str "GRAPH " var-or-iri " " (format-quads triples pretty?)))

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

(defmethod f/format-ast :load-update [{:keys [pretty?]} [_ load-update]]
  (if pretty?
    (cstr/join "\n" load-update)
    (cstr/join " " load-update)))

(defmethod f/format-ast :clear-update [{:keys [pretty?]} [_ clear-update]]
  (if pretty?
    (cstr/join "\n" clear-update)
    (cstr/join " " clear-update)))

(defmethod f/format-ast :drop-update [{:keys [pretty?]} [_ drop-update]]
  (if pretty?
    (cstr/join "\n" drop-update)
    (cstr/join " " drop-update)))

(defmethod f/format-ast :create-update [{:keys [pretty?]} [_ create-update]]
  (if pretty?
    (cstr/join "\n" create-update)
    (cstr/join " " create-update)))

(defmethod f/format-ast :add-update [{:keys [pretty?]} [_ add-update]]
  (if pretty?
    (cstr/join "\n" add-update)
    (cstr/join " " add-update)))

(defmethod f/format-ast :move-update [{:keys [pretty?]} [_ move-update]]
  (if pretty?
    (cstr/join "\n" move-update)
    (cstr/join " " move-update)))

(defmethod f/format-ast :copy-update [{:keys [pretty?]} [_ copy-update]]
  (if pretty?
    (cstr/join "\n" copy-update)
    (cstr/join " " copy-update)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Graph Management specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod f/format-ast :using [_ [_ using]]
  (str "USING " using))

(defmethod f/format-ast :with [_ [_ with]]
  (str "WITH " with))

(defmethod f/format-ast :insert-data [{:keys [pretty?]} [_ insert-data]]
  (str "INSERT DATA " (format-quads insert-data pretty?)))

(defmethod f/format-ast :delete-data [{:keys [pretty?]} [_ delete-data]]
  (str "DELETE DATA " (format-quads delete-data pretty?)))

(defmethod f/format-ast :delete-where [{:keys [pretty?]} [_ delete-where]]
  (str "DELETE WHERE " (format-quads delete-where pretty?)))

(defmethod f/format-ast :delete [{:keys [pretty?]} [_ delete]]
  (str "DELETE " (format-quads delete pretty?)))

(defmethod f/format-ast :insert [{:keys [pretty?]} [_ insert]]
  (str "INSERT " (format-quads insert pretty?)))

(defmethod f/format-ast :insert-data-update [{:keys [pretty?]} [_ id-update]]
  (if pretty?
    (cstr/join "\n" id-update)
    (cstr/join " " id-update)))

(defmethod f/format-ast :delete-data-update [{:keys [pretty?]} [_ dd-update]]
  (if pretty?
    (cstr/join "\n" dd-update)
    (cstr/join " " dd-update)))

(defmethod f/format-ast :delete-where-update [{:keys [pretty?]} [_ dw-update]]
  (if pretty?
    (cstr/join "\n" dw-update)
    (cstr/join " " dw-update)))

(defmethod f/format-ast :modify-update [{:keys [pretty?]} [_ mod-update]]
  (if pretty?
    (cstr/join "\n" mod-update)
    (cstr/join " " mod-update)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Updates
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod f/format-ast :update-request [{:keys [pretty?]} [_ updates]]
  (if pretty?
    (cstr/join ";\n" updates)
    (cstr/join "; " updates)))
