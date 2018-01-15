class CreateUserInterfaceFactory {
	static CreateUserInterface create(String dbName = "Users", String userSecretsFilename = "user.groovy", String adminSecretsFilename = "adminSecrets.groovy", MyLogger logger = new MyLogger()) {
		def userConfigHolder = userConfigHolderFromFilename(dbName, userSecretsFilename, logger)
		return doCreate(userConfigHolder, adminSecretsFilename, dbName, logger)
	}

	static CreateUserInterface createWithUserSecretsProvider(String dbName, SecretsProvider userSecretsProvider, adminSecretsFileName, logger) {
		def userConfigHolder = userConfigHolderFromProvider(dbName, userSecretsProvider, logger)
		return doCreate(userConfigHolder, adminSecretsFileName, dbName, logger)
	}

	private
	static doCreate(DbUserConfigHolder userConfigHolder, String adminSecretsFilename, String dbName, MyLogger logger) {
		new CreateUserInterface(
				userConfigHolder: userConfigHolder,
				adminConfigHolder: new DbAdminConfigHolder(secretsFilename: adminSecretsFilename, dbName: dbName, logger: logger),
				logger: logger
		)
	}

	private static userConfigHolderFromFilename(String dbName, String userSecretsFilename, MyLogger logger) {
		new DbUserConfigHolder(dbName: dbName, secretsFilename: userSecretsFilename, logger: logger)
	}

	private static userConfigHolderFromProvider(String dbName, SecretsProvider userSecretsProvider, logger) {
		return new DbUserConfigHolder(
				dbName: dbName,
				secretsProvider: userSecretsProvider,
				logger: logger
		)
	}

}
