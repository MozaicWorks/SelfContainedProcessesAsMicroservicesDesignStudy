import groovy.sql.Sql

class MySqlBuilder {
	String baseUrl = "jdbc:mysql://localhost:3306"
	String dbName
	def secretsProvider
	String driver = "com.mysql.jdbc.Driver"

	Sql getSql() {
		def sql = Sql.newInstance(
				dbUrl(),
				secretsProvider.username,
				secretsProvider.password,
				driver
		)
		sql.connection.autoCommit = false
		return sql
	}

	def dbUrl() {
		return this.dbName ? "$baseUrl/${this.dbName}" : baseUrl
	}

}

