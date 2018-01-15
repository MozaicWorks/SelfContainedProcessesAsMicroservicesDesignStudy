class DbAdminConfigHolder {
	def dbName
	def logger
	def secretsFilename

	SecretsProvider getSecretsProvider() {
		return new SecretsProviderFromConfigFile(
				fileLocation: new File(secretsFilename).toURL()
		)
	}

	final DatabaseSetup getDatabaseSetup() {
		return new DatabaseSetup(
				databaseOperations:
						new DatabaseOperations(
								adminSqlProvider:
										new MySqlProvider(
												secretsProvider: secretsProvider,
												logger: logger
										),
								dbName: dbName,
								logger: logger
						),
				logger: logger
		)
	}
}
