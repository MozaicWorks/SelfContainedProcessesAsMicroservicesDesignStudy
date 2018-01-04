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
}
