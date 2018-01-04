class SecretsProviderInMemory implements SecretsProvider {
	def username
	def password

	String getUsername() {
		return username
	}

	String getPassword() {
		return password
	}
}
