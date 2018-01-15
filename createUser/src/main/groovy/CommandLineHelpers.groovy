class CommandLineHelpers {
	static exitWithSuccess() {
		println "OK"
		System.exit(0)
	}

	static exitWithError(message) {
		println("ERROR: ${message}")
		System.exit(-1)
	}
}
