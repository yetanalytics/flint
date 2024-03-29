(ns com.yetanalytics.flint.sparql
  (:require [clojure.java.io :as io]
            [criterium.core :as crit])
  (:import [org.apache.jena.query QueryFactory]
           [org.apache.jena.update UpdateFactory]))

;; Ensure that outputs are indeed valid SPARQL (at least according to
;; Apache Jena).

(defmacro make-valid-sparql-tests [parse-fn dir-name]
  (let [files# (->> dir-name io/file file-seq (filter #(.isFile %)))
        tests# (map (fn [file#]
                      (let [fstr# (slurp file#)]
                        `(~parse-fn ~fstr#)))
                    files#)]
    `(do ~@tests#
         nil)))

(defn valid-queries-test
  []
  (make-valid-sparql-tests (fn [q] (QueryFactory/create q))
                           "dev-resources/test-fixtures/outputs/query/"))

(defn valid-updates-test
  []
  (make-valid-sparql-tests (fn [u] (UpdateFactory/create u))
                           "dev-resources/test-fixtures/outputs/update/"))

(defn valid-update-requests-test
  []
  (make-valid-sparql-tests (fn [u] (UpdateFactory/create u))
                           "dev-resources/test-fixtures/outputs/update-request/"))

(comment
  (valid-queries-test)
  (valid-updates-test)
  (valid-update-requests-test))

;; Query/Update writing sandbox

(comment
  (QueryFactory/create
   "CONSTRUCT { _:b0 ?pred _:b0 }
    WHERE { _:b0 ?pred _:b0 .
            OPTIONAL {
              ?y ?pred2 _:b1
            }
            _:b0 ?question _:b0 . }")
  
  (QueryFactory/create
   "SELECT ?x
    WHERE {
      { ?x <http://foo.org> _:1 . }
      { ?y <http://bar.org> _:1 . }
    }")
  
  (QueryFactory/create
   "PREFIX foo: <http://foo.org>
    SELECT ?x WHERE {
      ?x foo:bar _:1 .
      FILTER NOT EXISTS { ?z foo:baz ?w . }
      ?y foo:qux _:1 .
    }")
  
  (QueryFactory/create
   "SELECT ?x WHERE {
      [<http://bar.org#1> [<http://bar.org#2> ?x]] <http://foo.org> \"w\"
   }")
  )

(comment
  (QueryFactory/create
   "SELECT ((AVG(MAX(?x)) + 1) AS ?avg)
    WHERE { ?x ?y ?z . }")
  
  (QueryFactory/create
   "SELECT (SUM(?x + ?y) AS ?sum) (str(?sum) AS ?z2)
    WHERE {
      ?x ?y ?z .
    }
    ORDER BY ?x")
  (QueryFactory/create
   "SELECT ((?z + SUM(?y + ?x)) AS ?sum) (?sum AS ?z2)
    WHERE {
      ?x ?y ?z .
    }
    GROUP BY ?z")
  (QueryFactory/create
   "SELECT ?q
    WHERE {
      ?x ?y ?z .
    }
    GROUP BY ?q")

  (QueryFactory/create
   "SELECT (COUNT(DISTINCT ?z) AS ?z)
    WHERE { ?x a <http://foo.org/Thing> }")

  (println
   (QueryFactory/create
    "SELECT ?x
     WHERE {
      { SELECT (?z AS ?x) WHERE { 
         SELECT ?x WHERE { ?x ?y ?z }
       } }
      { SELECT (?z AS ?x) WHERE { ?y a <http://foo.org/Thing2>. } }
    }"))

  (println
   (QueryFactory/create
    "SELECT ?x
     WHERE {
      SELECT (?z AS ?x) WHERE {
        SELECT ?x WHERE {?x a <http://foo.org/Thing1> . } }
    }"))
  )

(comment
  (UpdateFactory/create
   "PREFIX foo: <http://foo.org/>
    DELETE WHERE { ?x foo:bar ?y } ;
    
    PREFIX foo: <http://foo-bar.org/>
    PREFIX fii: <http://fii.org/>
    DELETE WHERE { ?x foo:bar ?y . ?z fii:fum ?w }")

  (UpdateFactory/create
   "BASE <http://foo.org/>
    PREFIX bar: <bar>
    DELETE WHERE { ?x bar:qux ?y . ?z <quu> ?w . } ;
    
    BASE <http://fii.org/>
    PREFIX bii: <bii>
    BASE <http://foe.org/>
    PREFIX boe: <boe>

    DELETE WHERE { ?x boe:goe ?y . ?z <joe> ?w . }")

  (UpdateFactory/create
   "BASE <http://foo.org/>
    DELETE WHERE { ?x <bar> ?y } ;
    
    BASE <http://baz.org/>
    DELETE WHERE { ?x <qux> ?y }"))

(comment
  (UpdateFactory/create
   "CREATE GRAPH <http://graphs.com/1>;
    LOAD <http://foo.org> INTO GRAPH <http://graphs.com/2>;
    COPY GRAPH <http://graphs.com/1> TO GRAPH <http://graphs.com/2>;
    COPY DEFAULT TO GRAPH <http://graphs.com/2>;
    COPY GRAPH <http://graphs.com/1> TO DEFAULT;
    ADD GRAPH <http://graphs.com/1> TO GRAPH <http://graphs.com/2>;
    ADD DEFAULT TO GRAPH <http://graphs.com/2>;
    ADD GRAPH <http://graphs.com/1> TO DEFAULT;
    MOVE GRAPH <http://graphs.com/2> TO DEFAULT;
    MOVE DEFAULT TO GRAPH <http://graphs.com/2>;
    MOVE GRAPH <http://graphs.com/1> TO GRAPH <http://graphs.com/2>;
    CLEAR GRAPH <http://graphs.com/2>;
    CLEAR DEFAULT;
    CLEAR NAMED;
    CLEAR ALL;
    DROP GRAPH  <http://graphs.com/2>;
    DROP DEFAULT;
    DROP NAMED;
    DROP ALL;")
  
  (UpdateFactory/create
   "CREATE SILENT GRAPH <http://graphs.com/1>;
    LOAD SILENT <http://foo.org> INTO GRAPH <http://graphs.com/2>;
    COPY SILENT GRAPH <http://graphs.com/1> TO GRAPH <http://graphs.com/2>;
    COPY SILENT DEFAULT TO GRAPH <http://graphs.com/2>;
    COPY SILENT GRAPH <http://graphs.com/1> TO DEFAULT;
    ADD SILENT GRAPH <http://graphs.com/1> TO GRAPH <http://graphs.com/2>;
    ADD SILENT DEFAULT TO GRAPH <http://graphs.com/2>;
    ADD SILENT GRAPH <http://graphs.com/1> TO DEFAULT;
    MOVE SILENT GRAPH <http://graphs.com/2> TO DEFAULT;
    MOVE SILENT GRAPH <http://graphs.com/1> TO GRAPH <http://graphs.com/2>;
    CLEAR SILENT GRAPH  <http://graphs.com/2>;
    CLEAR SILENT DEFAULT;
    CLEAR SILENT NAMED;
    CLEAR SILENT ALL;
    DROP SILENT GRAPH  <http://graphs.com/2>;
    DROP SILENT DEFAULT;
    DROP SILENT NAMED;
    DROP SILENT ALL;"))

(comment
  (require '[com.yetanalytics.flint :as flint])

  (crit/quick-bench
   (flint/format-query
    {:prefixes {:foo "<http://foo.com/>"}
     :select ['?x]
     :where [['?x :foo/bar "eleventy-six"]]}))

  (dotimes [_ 10000]
    (flint/format-query
     {:prefixes {:foo "<http://foo.com/>"}
      :select ['?x]
      :where [['?x :foo/bar "eleventy-six"]]}))
  
  (crit/quick-bench
   (flint/format-query
    {:prefixes {:foo "<http://foo.com/>"}
     :select ['?x]
     :where [['?x :foo/oooooooooone "ONE"]
             ['?x :foo/ttttttttttwo "TWO"]
             ['?x :foo/ththththree "THREE"]
             ['?x :foo/ffffffffour "FOUR"]
             ['?x :foo/ffffffffive "FIVE"]
             ['?x :foo/sssssssssix "SIX"]
             ['?x :foo/ssssssseven "SEVEN"]
             ['?x :foo/eiiiiiiight "EIGHT"]
             ['?x :foo/niiiiiiiine "NINE"]
             ['?x :foo/ttttttttten "TEN"]]}
    :validate? false))
  )

(comment
  (require '[com.yetanalytics.flint.axiom.impl.validation :as v])

  ;; ASCII bench
  (crit/quick-bench
   (re-matches #"\?\w+" (name '?foo)))

  (crit/quick-bench
   (re-matches v/var-regex (name '?foo)))

  (crit/quick-bench
   (v/valid-var-symbol? '?foo))

  ;; Non-ASCII bench
  (crit/quick-bench
   (re-matches v/var-regex (name '?我们去西安吃面)))

  (crit/quick-bench
   (v/valid-var-symbol? '?我们今天吃西安炒面))

  ;; String literal bench
  (crit/quick-bench
   (re-matches v/literal-regex
               "\\\"supercalifragilisticexpialidocious\\\""))

  (crit/quick-bench
   (v/valid-string-literal? "\\\"supercalifragilisticexpialidocious\\\""))

  ;; Prefix name bench
  (crit/bench
   (re-matches v/prefix-name-regex (name :foo%80bar)))

  (crit/bench
   (v/valid-prefix-keyword? :foo%80bar)))
