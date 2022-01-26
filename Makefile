.phony: test-clj test-cov

# TODO: test-cljs

test-clj:
	clojure -X:test:run-clj

test-cov:
	clojure -X:test:run-cov

ci: test-clj test-cov
