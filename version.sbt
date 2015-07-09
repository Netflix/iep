version in ThisBuild := sys.env getOrElse ("TRAVIS_TAG", "x.y-SNAPPSHOT")
