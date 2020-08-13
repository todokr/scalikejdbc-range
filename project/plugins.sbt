libraryDependencies += "org.postgresql" % "postgresql" % "42.1.4"

logLevel := Level.Warn
addSbtPlugin("org.scalikejdbc" %% "scalikejdbc-mapper-generator" % "3.5.0")
