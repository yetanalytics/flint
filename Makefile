.phony: test-clj test-cov

# TODO: test-cljs

test-clj:
	clj -X:test:run-clj

test-cov:
	clj -X:test:run-cov

ci: test-clj test-cov

