import groovy.sql.Sql

class MySqlProvider {
	String baseUrl = "jdbc:mysql://localhost:3306"
	String dbName
	def secretsProvider
	String driver = "com.mysql.jdbc.Driver"
	String parameters = "useSSL=false"
	int updateCount

	private String dbUrl() {
		return dbName ? "$baseUrl/$dbName?$parameters" : "$baseUrl?$parameters"
	}

	def executeWithInstance(Closure action) {
		def result
		Sql.withInstance(
				dbUrl(),
				secretsProvider.username,
				secretsProvider.password,
				driver,
				{ sql ->
					result = action(sql)
					this.updateCount = sql.updateCount
				}
		)
		return result
	}

	def execute(commandString) {
		executeWithInstance { sql ->
			sql.execute(commandString)
		}
	}

	def executeUpdate(commandString) {
		executeWithInstance { sql ->
			sql.executeUpdate(commandString)
		}
	}

	def firstRow(def commandString) {
		executeWithInstance { sql ->
			sql.firstRow(commandString)
		}
	}

	Result executeSqlCommand(commandString, actionName, expectedUpdateCount) {
		Result result = tryCatchAll(
				{ doExecute(commandString, expectedUpdateCount, actionName) },
				{ ex -> Result.fromException(ex, actionName) }
		)
		result.log(logger)
		return result
	}

	private Result doExecute(commandString, expectedUpdateCount, actionName) {
		sqlProvider.execute(commandString)

		return (expectedUpdateCount == sqlProvider.updateCount) ?
				Result.success(actionName) :
				Result.wrongUpdateCount(actionName, expectedUpdateCount, sqlProvider.updateCount)
	}

	private tryCatchAll(Closure action, Closure exceptionReaction) {
		try {
			action()
		} catch (Exception ex) {
			exceptionReaction(ex)
		}
	}

}