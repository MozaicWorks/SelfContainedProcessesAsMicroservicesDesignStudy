@Grab("org.codehaus.gpars:gpars:1.2.0")
@Grab("org.apache.commons:commons-lang3:3.7")
import static groovyx.gpars.GParsPool.*
import org.apache.commons.lang3.RandomStringUtils


def numberOfCreates = 100

withPool {
	def times = (1..numberOfCreates).collectParallel {
		final long startTime = System.currentTimeMillis()
		runProcess('groovyclient CreateUser.groovy -create', 2)
		return System.currentTimeMillis() - startTime
	}

	println "MIN: ${times.min()}"
	println "MAX: ${times.max()}"
	println "AVERAGE: ${times.sum() / times.size()}"

	println "All times: ${times.join(', ')}"
}

private runProcess(executablePath, numberOfCreates) {
	def logger = new MyLogger()
	def serr = new StringBuilder()
	Process proc = executablePath.execute()
	def reader = proc.in.newReader()

	try {
		def outputStream = proc.outputStream
		create(outputStream, reader, logger)
	} finally {
		proc.consumeProcessErrorStream(serr)
		reader.close()
		proc.waitFor()
	}
}

private create(outputStream, inputReader, logger) {
	def args = [
			[
					line : "First Name:",
					value: RandomStringUtils.randomAlphabetic(100)
			],
			[
					line : "Last Name:",
					value: RandomStringUtils.randomAlphabetic(100)
			]
	]

	return executeCommand(outputStream, args, logger, inputReader)
}

private executeCommand(outputStream, args, logger, inputReader) {
	args.each { arg ->
		doArgument(inputReader, outputStream, arg.line, arg.value, logger)
	}
	doOK(inputReader)
}

private doOK(inputReader) {
	line = inputReader.readLine()
	if (line == "OK") println "OK"
}

private doArgument(reader, outputStream, argumentLine, argumentValue, logger) {
	def line = reader.readLine()
	logger.info "Got $line"
	if (line == argumentLine) {
		logger.info "Sending $argumentValue"
		sendString(outputStream, argumentValue)
		logger.info("Sent $argumentLine $argumentValue")
	}
}

private sendString(stream, theString) {
	stream.write((theString + "\n").getBytes())
	stream.flush()
}

class MyLogger {
	def info(message) {
		println message
	}
}