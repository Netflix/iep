# Map stdin to /dev/null to avoid interactive prompts if there is some failure related to the
# build script.
SBT := cat /dev/null | project/sbt

IVY_CACHE_URL := https://www.dropbox.com/s/fkv9hscqskyxwgc/iep.tar.gz?dl=0

.PHONY: build coverage license get-ivy-cache

ifeq (${TRAVIS_PULL_REQUEST},false)
travis: get-ivy-cache publish
else
travis: get-ivy-cache build
endif

build:
	$(SBT) clean test checkLicenseHeaders

publish:
	$(SBT) clean test checkLicenseHeaders storeBintrayCredentials publish bintrayRelease

coverage:
	$(SBT) clean coverage test coverageReport
	$(SBT) coverageAggregate

license:
	$(SBT) formatLicenseHeaders

get-ivy-cache:
	curl -L $(IVY_CACHE_URL) -o $(HOME)/ivy.tar.gz
	tar -C $(HOME) -xzf $(HOME)/ivy.tar.gz
