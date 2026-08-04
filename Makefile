# Map stdin to /dev/null to avoid interactive prompts if there is some failure related to the
# build script.
SBT := cat /dev/null | project/sbt

.PHONY: build snapshot release coverage format

build:
	echo "Starting build"
	$(SBT) clean testFull checkLicenseHeaders

snapshot:
	echo "Starting snapshot build"
	git fetch --unshallow --tags
	$(SBT) storeBintrayCredentials
	$(SBT) clean testFull checkLicenseHeaders publish

release:
	# Storing the bintray credentials needs to be done as a separate command so they will
	# be available early enough for the publish task.
	#
	# The storeBintrayCredentials still needs to be on the subsequent command or we get:
	# [error] (iep-service/*:bintrayEnsureCredentials) java.util.NoSuchElementException: None.get
	echo "Starting release build"
	git fetch --unshallow --tags
	$(SBT) storeBintrayCredentials
	$(SBT) clean testFull checkLicenseHeaders storeBintrayCredentials publish bintrayRelease

coverage:
	$(SBT) clean coverage testFull coverageReport
	$(SBT) coverageAggregate

format:
	$(SBT) formatLicenseHeaders

