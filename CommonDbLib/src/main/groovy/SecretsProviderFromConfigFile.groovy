class SecretsProviderFromConfigFile implements SecretsProvider {
	def config = new ConfigSlurper()
	URL fileLocation

	String getUsername() {
		def secrets = config.parse(fileLocation)
		return secrets.username
	}

	String getPassword() {
		def secrets = config.parse(fileLocation)
		return secrets.password
	}
}
