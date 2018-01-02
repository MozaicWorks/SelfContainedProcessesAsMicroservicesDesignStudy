@Grab("org.codehaus.gpars:gpars:1.2.0")
@Grab("org.apache.commons:commons-lang3:3.7")
import static groovyx.gpars.GParsPool.*
import org.apache.commons.lang3.RandomStringUtils

runProcess('./CreateUser.groovy -daemon', 100)

private runProcess(executablePath, numberOfCreates) {
	def logger = new MyLogger()
	def serr = new StringBuilder()
	Process proc = executablePath.execute()

	try {
		def outputStream = proc.outputStream
		def reader = proc.in.newReader()

		def times = (1..numberOfCreates).collect {
			create(outputStream, reader, logger)
		}
		println "MIN: ${times.min()}"
		println "MAX: ${times.max()}"
		println "AVERAGE: ${times.sum() / times.size()}"

	}
	finally {
		proc.consumeProcessErrorStream(serr)
		sendString(outputStream, "exit", logger)
		reader.close()
		proc.outputStream.flush()
		proc.waitFor()
	}

}

private create(outputStream, inputReader, logger) {
	def command = [
			name: "create",
			args: [
					[
							line : "First name:",
							value: RandomStringUtils.randomAlphabetic(100)
					],
					[
							line : "Last name:",
							value: RandomStringUtils.randomAlphabetic(100)
					]
			]
	]

	return executeCommand(outputStream, command, logger, inputReader)
}

private executeCommand(outputStream, command, logger, inputReader) {
	final long startTime = System.currentTimeMillis()
	sendString(outputStream, command.name, logger)
	command.args.each { arg ->
		doArgument(inputReader, outputStream, arg.line, arg.value, logger)
	}
	doOK(inputReader)
	def finalTime = System.currentTimeMillis() - startTime
	println "DONE: " + finalTime
	return finalTime
}

private doOK(inputReader) {
	line = inputReader.readLine()
	if (line == "OK") println "OK"
}

private doArgument(reader, outputStream, argumentLine, argumentValue, logger) {
	def line = reader.readLine()
	if (line == argumentLine) {
		sendString(outputStream, argumentValue, logger)
		logger.info("Sent $argumentLine $argumentValue")
	}
}

private doLastName(reader, outputStream, lastName, MyLogger logger) {
	doArgument(reader, outputStream, "Last name:", lastName, logger)
}

private sendString(stream, String theString, logger) {
	stream.write((theString + "\n").getBytes())
	stream.flush()
}

class MyLogger {
	def info(message) {
		println message
	}
}