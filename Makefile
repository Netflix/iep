SBT := project/sbt

build:
	$(SBT) clean test checkLicenseHeaders

coverage:
	$(SBT) clean coverage test coverageReport
	$(SBT) coverageAggregate

license:
	$(SBT) formatLicenseHeaders
