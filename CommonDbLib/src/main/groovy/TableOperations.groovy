class TableOperations {

	SqlProvider sqlProvider
	def logger

	def createTable(String tableName, String createTableCommand) {
		if (!existsTable(sqlProvider, tableName)) {
			sqlProvider.executeSqlCommand(
					createTableCommand,
					"createTable $tableName",
					0
			)
		} else {
			logger.info("Table $tableName exists; nothing to do")
		}
	}

	def existsTable(sqlProvider, tableName) {
		def rows = sqlProvider.firstRow("SHOW TABLES LIKE $tableName;")
		return (rows?.size() == 1)
	}
}
