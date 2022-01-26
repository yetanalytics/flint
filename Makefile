.phony: test-clj

# TODO: test-cljs

test-clj:
	clj -X:test :dirs '["src/test"]'

