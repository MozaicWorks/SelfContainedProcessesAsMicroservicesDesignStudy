class DbUserConfigHolder {
	def dbName
	def logger
	SecretsProvider secretsProvider

	DbUserConfigHolder(params) {
		this.dbName = params.dbName
		this.logger = params.logger

		if (params.secretsFilename) {
			this.secretsProvider = secretsProviderFromConfigFile(params.secretsFilename)
		} else {
			this.secretsProvider = params.secretsProvider
		}
	}

	private SecretsProvider getSecretsProvider() {
		this.secretsProvider
	}

	private static SecretsProvider secretsProviderFromConfigFile(secretsFilename) {
		return new SecretsProviderFromConfigFile(
				fileLocation: new File(secretsFilename).toURL()
		) as SecretsProvider
	}

	SqlProvider getSqlProvider() {
		new MySqlProvider(
				dbName: dbName,
				secretsProvider: secretsProvider,
				logger: logger
		)
	}
}
