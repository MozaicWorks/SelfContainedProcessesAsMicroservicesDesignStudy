class CommandLineHelpers {
	static void printError(message) {
		println "ERROR: $message"
	}

	static resultOK() {
		println "OK"
	}

	static exitWithError() {
		throw new RuntimeException()
	}
}
