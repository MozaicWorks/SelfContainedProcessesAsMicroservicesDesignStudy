class DatabaseSetup {
	def logger
	DatabaseOperations databaseOperations

	def createDatabaseUserAndSchema(
			SecretsProvider userSecretsProvider,
			Closure createDatabaseSchema) {
		logger.info("START self setup")

		databaseOperations.createDatabase()
		databaseOperations.createUser(userSecretsProvider)
		createDatabaseSchema()

		logger.info("END self setup")
	}

	def dropDatabaseAndUser(username) {
		logger.info("START self cleanup")

		databaseOperations.dropDatabase()
		databaseOperations.dropUser(username)

		logger.info("END self cleanup")
	}

	def backup(SecretsProvider secretsProvider, dbName) {
		logger.info("START backup")

		def postfix = new Date().format("YYYYMMDDHHSS")
		def filename = "backup-${postfix}.sql"
		def serr = new StringBuilder()
		def sout = new StringBuilder()

		// create file that allows passwordless login
		def configFile = new File(System.getProperty("user.home"), "/.my.cnf")
		configFile.createNewFile()
		configFile.write("""\
[mysqldump]
password=${secretsProvider.password}
""")
		def process = ["mysqldump", "-u${secretsProvider.username}", "$dbName"].execute()
		process.waitFor()
		process.consumeProcessErrorStream(serr)
		process.consumeProcessOutputStream(sout)

		def backupFile = new File(filename)
		backupFile.write(sout.toString())

		def success = (process.exitValue() == 0)

		if (success) {
			logger.info("SUCCESS: Backed up to $filename")
		} else {
			logger.info("ERROR: Backup failed")
		}

		configFile.delete()
		if (serr.toString().size() > 0) {
			logger.severe(serr)
		}
		return success
	}
}
