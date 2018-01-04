class MyLogger {
	String dateTimeFormat = "DD/MM/YYYY HH:mm:ss"
	PrintStream logDestinationStream = System.err

	def info(message) {
		log("INFO", message)
	}

	def severe(message) {
		log("SEVERE", message)
	}

	def log(prefix, message) {
		logDestinationStream.println "${formatDate()} $prefix: $message"
	}

	def formatDate() {
		new Date().format(dateTimeFormat)
	}
}
