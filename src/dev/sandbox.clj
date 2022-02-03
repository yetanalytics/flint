(ns sandbox
  (:import [org.apache.jena.update UpdateFactory]))

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
    DELETE WHERE { ?x <qux> ?y }")
  )
