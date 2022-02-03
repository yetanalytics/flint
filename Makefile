.phony: test-clj test-cov

test-clj:
	clojure -X:test:run-clj

test-cljs:
	clojure -M:test:run-cljs

test-cov:
	clojure -X:test:run-cov

ci: test-clj test-cljs test-cov
