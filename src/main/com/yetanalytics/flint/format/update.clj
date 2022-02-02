(ns com.yetanalytics.flint.format.update
  (:require [clojure.string :as cstr]
            [com.yetanalytics.flint.format :as f]
            [com.yetanalytics.flint.format.axiom]
            [com.yetanalytics.flint.format.prologue]
            [com.yetanalytics.flint.format.triple]
            [com.yetanalytics.flint.format.where]))

(defn- format-quads [quads pretty?]
  (-> quads
      (f/join-clauses pretty?)
      (f/wrap-in-braces pretty?)))

(defmethod f/format-ast-node :update/kw [_ [_ kw]]
  (case kw
    :default "DEFAULT"
    :named   "NAMED"
    :all     "ALL"))

(defmethod f/format-ast-node :update/iri [_ [_ iri]]
  iri)

(defmethod f/format-ast-node :update/named-iri [_ [_ [_ iri]]]
  (str "NAMED " iri))

(defmethod f/format-ast-node :update/default-graph [_ _]
  "DEFAULT")

(defmethod f/format-ast-node :update/named-graph [_ [_ iri]]
  iri)

(defmethod f/format-ast-node :triple/quads [{:keys [pretty?]} [_ [_ var-or-iri triples]]]
  (str "GRAPH " var-or-iri " " (format-quads triples pretty?)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Graph Management specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod f/format-ast-node :into [_ [_ in]]
  (str "INTO " in))

(defmethod f/format-ast-node :to [_ [_ to]]
  (str "TO " to))

(defmethod f/format-ast-node :load [_ [_ ld]]
  (str "LOAD " ld))

(defmethod f/format-ast-node :load-silent [_ [_ ld-silent]]
  (str "LOAD SILENT " ld-silent))

(defmethod f/format-ast-node :clear [_ [_ clr]]
  (str "CLEAR " clr))

(defmethod f/format-ast-node :clear-silent [_ [_ clr-silent]]
  (str "CLEAR SILENT " clr-silent))

(defmethod f/format-ast-node :drop [_ [_ drp]]
  (str "DROP " drp))

(defmethod f/format-ast-node :drop-silent [_ [_ drp-silent]]
  (str "DROP SILENT " drp-silent))

(defmethod f/format-ast-node :create [_ [_ create]]
  (str "CREATE " create))

(defmethod f/format-ast-node :create-silent [_ [_ create-silent]]
  (str "CREATE SILENT " create-silent))

(defmethod f/format-ast-node :add [_ [_ add]]
  (str "ADD " add))

(defmethod f/format-ast-node :add-silent [_ [_ add-silent]]
  (str "ADD SILENT " add-silent))

(defmethod f/format-ast-node :move [_ [_ move]]
  (str "MOVE " move))

(defmethod f/format-ast-node :move-silent [_ [_ move-silent]]
  (str "MOVE SILENT " move-silent))

(defmethod f/format-ast-node :copy [_ [_ copy]]
  (str "COPY " copy))

(defmethod f/format-ast-node :copy-silent [_ [_ copy-silent]]
  (str "COPY SILENT " copy-silent))

(defmethod f/format-ast-node :load-update [{:keys [pretty?]} [_ load-update]]
  (f/join-clauses load-update pretty?))

(defmethod f/format-ast-node :clear-update [{:keys [pretty?]} [_ clear-update]]
  (f/join-clauses clear-update pretty?))

(defmethod f/format-ast-node :drop-update [{:keys [pretty?]} [_ drop-update]]
  (f/join-clauses drop-update pretty?))

(defmethod f/format-ast-node :create-update [{:keys [pretty?]} [_ create-update]]
  (f/join-clauses create-update pretty?))

(defmethod f/format-ast-node :add-update [{:keys [pretty?]} [_ add-update]]
  (f/join-clauses add-update pretty?))

(defmethod f/format-ast-node :move-update [{:keys [pretty?]} [_ move-update]]
  (f/join-clauses move-update pretty?))

(defmethod f/format-ast-node :copy-update [{:keys [pretty?]} [_ copy-update]]
  (f/join-clauses copy-update pretty?))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Graph Management specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod f/format-ast-node :using [_ [_ using]]
  (str "USING " using))

(defmethod f/format-ast-node :with [_ [_ with]]
  (str "WITH " with))

(defmethod f/format-ast-node :insert-data [{:keys [pretty?]} [_ insert-data]]
  (str "INSERT DATA " (format-quads insert-data pretty?)))

(defmethod f/format-ast-node :delete-data [{:keys [pretty?]} [_ delete-data]]
  (str "DELETE DATA " (format-quads delete-data pretty?)))

(defmethod f/format-ast-node :delete-where [{:keys [pretty?]} [_ delete-where]]
  (str "DELETE WHERE " (format-quads delete-where pretty?)))

(defmethod f/format-ast-node :delete [{:keys [pretty?]} [_ delete]]
  (str "DELETE " (format-quads delete pretty?)))

(defmethod f/format-ast-node :insert [{:keys [pretty?]} [_ insert]]
  (str "INSERT " (format-quads insert pretty?)))

(defmethod f/format-ast-node :insert-data-update [{:keys [pretty?]} [_ id-update]]
  (f/join-clauses id-update pretty?))

(defmethod f/format-ast-node :delete-data-update [{:keys [pretty?]} [_ dd-update]]
  (f/join-clauses dd-update pretty?))

(defmethod f/format-ast-node :delete-where-update [{:keys [pretty?]} [_ dw-update]]
  (f/join-clauses dw-update pretty?))

(defmethod f/format-ast-node :modify-update [{:keys [pretty?]} [_ mod-update]]
  (f/join-clauses mod-update pretty?))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Updates
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn join-updates
  "Given a coll of `updates`, return a semicolon-joined UpdateRequest string."
  [updates pretty?]
  (if pretty?
    (cstr/join ";\n" updates)
    (cstr/join "; " updates)))
