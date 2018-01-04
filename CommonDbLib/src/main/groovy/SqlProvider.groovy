trait SqlProvider {
	int updateCount
	def logger

	abstract Result executeSqlCommand(String commandString, String actionName, int expectedUpdateCount)

	abstract executeUpdate(commandString)

	abstract firstRow(commandString)

	def createTable(String tableName, String tableCommandString) {
		assert logger != null, "Null logger in SqlProviders"
		new TableOperations(sqlProvider: this, logger: this.logger).createTable(tableName, tableCommandString)
	}
}