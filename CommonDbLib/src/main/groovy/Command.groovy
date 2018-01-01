class Command {
	def logger
	def sql

	Result executeSqlCommand(commandString, actionName, expectedUpdateCount) {
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
