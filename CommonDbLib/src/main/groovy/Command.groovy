import groovy.sql.Sql

class Command {
	Result executeSqlCommand(def commandString, String actionName, int expectedUpdateCount, Sql sql, logger) {
		def result
		try {
			sql.execute(commandString)
			sql.commit()

			def actualUpdateCount = sql.updateCount
			if (expectedUpdateCount == actualUpdateCount) {
				result = Result.success(actionName)
			} else {
				result = Result.wrongUpdateCount(actionName, expectedUpdateCount, actualUpdateCount)
			}
		} catch (Exception ex) {
			sql.rollback()
			result = Result.fromException(ex, actionName)
		}
		result.log(logger)
		return result
	}

}
