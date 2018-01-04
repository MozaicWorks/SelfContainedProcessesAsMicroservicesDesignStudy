class ParameterReader {
	def scanner

	def readParameters(parameterMapping) {
		return parameterMapping.collectEntries { columnName, labelName ->
			readParameter(scanner, labelName, columnName)
		}
	}

	private readParameter(scanner, labelName, columnName) {
		println labelName
		def value = scanner.nextLine()
		return ["$columnName": value]
	}
}
