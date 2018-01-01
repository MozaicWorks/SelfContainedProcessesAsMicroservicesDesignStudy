import groovy.sql.Sql

class MySqlBuilder {
	String baseUrl = "jdbc:mysql://localhost:3306"
	String dbName
	def secretsProvider
	String driver = "com.mysql.jdbc.Driver"
	String parameters = "useSSL=false"

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
		return dbName ? "$baseUrl/$dbName?$parameters" : "$baseUrl?$parameters"
	}

}

