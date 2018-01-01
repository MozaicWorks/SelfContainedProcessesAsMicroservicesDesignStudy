class DatabaseOperations {
	private MySqlProvider adminSqlProvider
	private dbName
	private logger

	def createDatabase() {
		if (!databaseExists()) {
			adminSqlProvider.executeUpdate("CREATE DATABASE " + dbName)
			logger.info("SUCCESS: create database $dbName")
		} else {
			logger.info("Nothing to setup for database $dbName")
		}
	}

	def databaseExists() {
		def row = adminSqlProvider.firstRow("""\
	SELECT SCHEMA_NAME as schemaName FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = $dbName
"""
		)
		return (row?.schemaName == dbName)
	}

	def dropDatabase() {
		if (databaseExists()) {
			adminSqlProvider.executeUpdate("DROP DATABASE " + dbName)
			logger.info("SUCCESS: dropped database $dbName")
		} else {
			logger.info("Database $dbName doesn't exist")
		}
	}

	def userExists(username) {
		def row = adminSqlProvider.firstRow("""
	SELECT * FROM mysql.user WHERE user = $username
""")
		return (row?.user == username)
	}

	def createUser(userSecretsProvider) {
		def username = userSecretsProvider.username
		def password = userSecretsProvider.password

		if (!userExists(username)) {
			adminSqlProvider.executeUpdate("GRANT USAGE ON *.* TO $username@localhost IDENTIFIED BY $password")
			adminSqlProvider.executeUpdate("GRANT ALL PRIVILEGES ON " + dbName + ".* TO $username@localhost")
			adminSqlProvider.executeUpdate("FLUSH PRIVILEGES")
			logger.info("SUCCESS: created user $username")
		} else {
			logger.info("Nothing to setup for user $username")
		}
	}

	def dropUser(username) {
		if (userExists(username)) {
			this.adminSqlProvider.executeUpdate("DROP USER $username@localhost")
			this.logger.info("SUCCESS: dropped user $username")
		} else {
			logger.info("User $username doesn't exist. Nothing left to do")
		}
	}
}