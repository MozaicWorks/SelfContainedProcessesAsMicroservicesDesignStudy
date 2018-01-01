class Command {
	def logger
	def sqlProvider

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
