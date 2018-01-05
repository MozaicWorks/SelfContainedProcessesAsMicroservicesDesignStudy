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
		def postfix = new Date().format("YYYYMMDDHHSS")

		def filename = "$dbName/backup-${postfix}.sql"

		return backupToFile(secretsProvider, dbName, filename)
	}

	def restore(SecretsProvider secretsProvider, dbName) {
		logger.info("START restore")

		def path = "$dbName/"
		def file = new File(path).listFiles().max {
			it.lastModified()
		}
		logger.info("Found file $file.name")

		def mySqlPasswordlessLoginFile = new MySqlPasswordlessLoginFile(secretsProvider: secretsProvider)
		mySqlPasswordlessLoginFile.createForClient()

		logger.info("Running restore command")
		def command = ["mysql", "-u${secretsProvider.username}", "$dbName"]
		def result = executeCommandWithContent(command, file.text)
		def success = (result.exitValue == 0)

		if (success) {
			logger.info(result.standardOutput)
			logger.info("SUCCESS: Restored $file")
		} else {
			logger.severe(result.standardOutput)
			logger.severe(result.errorOutput)
			logger.info("ERROR: Restore failed")
		}

		mySqlPasswordlessLoginFile.delete()

		return success
	}

	private backupToFile(SecretsProvider secretsProvider, dbName, GString filename) {
		logger.info("START backup")

		def mySqlPasswordlessLoginFile = new MySqlPasswordlessLoginFile(secretsProvider: secretsProvider)
		mySqlPasswordlessLoginFile.createForDump()

		boolean success = doBackup(secretsProvider, dbName, filename)

		mySqlPasswordlessLoginFile.delete()
		return success
	}

	private doBackup(SecretsProvider secretsProvider, dbName, GString filename) {
		def command = ["mysqldump", "-u${secretsProvider.username}", "$dbName"]
		def result = executeCommand(command)
		def success = (result.exitValue == 0)

		if (success) {
			def backupFile = new File(filename)
			backupFile.createNewFile()
			backupFile.write(result.standardOutput.toString())

			logger.info("SUCCESS: Backed up to $filename")
		} else {
			logger.severe(result.errorOutput)
			logger.info("ERROR: Backup failed")
		}
		return success
	}

	private executeCommand(List<String> command) {
		def standardOutput = new StringBuilder()
		def errorOutput = new StringBuilder()

		def process = command.execute()
		process.waitFor()
		process.consumeProcessOutput(standardOutput, errorOutput)

		return [exitValue: process.exitValue(), standardOutput: standardOutput, errorOutput: errorOutput]
	}

	private executeCommandWithContent(List<String> command, String content) {
		def standardOutput = new StringBuilder()
		def errorOutput = new StringBuilder()

		def process = command.execute()
		logger.info("Writing content to the process")
		process.out.write(content.getBytes())
		process.out.close()
		logger.info("Done writing content to the process")
		process.waitFor()
		process.consumeProcessOutput(standardOutput, errorOutput)

		return [exitValue: process.exitValue(), standardOutput: standardOutput, errorOutput: errorOutput]
	}
}
