import groovy.sql.Sql
import static ErrorManagementClosures.tryCatchAll

class MySqlProvider implements SqlProvider {
	String baseUrl = "jdbc:mysql://localhost:3306"
	String dbName
	SecretsProvider secretsProvider
	String driver = "com.mysql.jdbc.Driver"
	String parameters = "useSSL=false"

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

	Result executeSqlCommand(String commandString, String actionName, int expectedUpdateCount) {
		Result result = tryCatchAll(
				{ doExecute(commandString, expectedUpdateCount, actionName) },
				{ Exception ex -> Result.fromException(ex, actionName) }
		)
		result.log(logger)
		return result
	}

	private Result doExecute(String commandString, int expectedUpdateCount, String actionName) {
		execute(commandString)

		return (expectedUpdateCount == updateCount) ?
				Result.success(actionName) :
				Result.wrongUpdateCount(actionName, expectedUpdateCount, updateCount)
	}

	private String dbUrl() {
		return dbName ? "$baseUrl/$dbName?$parameters" : "$baseUrl?$parameters"
	}

	private executeWithInstance(Closure action) {
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
}