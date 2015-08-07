# Map stdin to /dev/null to avoid interactive prompts if there is some failure related to the
# build script.
SBT := cat /dev/null | project/sbt

.PHONY: build coverage license

ifeq (${TRAVIS_PULL_REQUEST},false)
ifeq (${TRAVIS_TAG},)
travis: publish
else
travis: release
endif
else
travis: build
endif

build:
	echo "Starting build"
	$(SBT) clean test checkLicenseHeaders

publish:
	echo "Starting publish"
	$(SBT) clean test checkLicenseHeaders storeBintrayCredentials publish

release:
	# Storing the bintray credentials needs to be done as a separate command so they will
	# be available early enough for the publish task.
	#
	# The storeBintrayCredentials still needs to be on the subsequent command or we get:
	# [error] (iep-service/*:bintrayEnsureCredentials) java.util.NoSuchElementException: None.get
	echo "Starting release"
	$(SBT) storeBintrayCredentials
	$(SBT) clean test checkLicenseHeaders storeBintrayCredentials publish bintrayRelease

coverage:
	$(SBT) clean coverage test coverageReport
	$(SBT) coverageAggregate

license:
	$(SBT) formatLicenseHeaders

