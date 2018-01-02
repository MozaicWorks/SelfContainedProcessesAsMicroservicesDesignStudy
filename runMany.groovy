@Grab("org.codehaus.gpars:gpars:1.2.0")
@Grab("org.apache.commons:commons-lang3:3.7")
import static groovyx.gpars.GParsPool.*
import org.apache.commons.lang3.RandomStringUtils

def executablePath = './CreateUser.groovy -create'

//withPool {
//	def times = (1..1).collectParallel {
		def result = runProcess(executablePath)
		println "exit value: $result.exitValue"
		println "out >"
		println result.output
		println "err >"
		println result.error
		println "Duration: ${result.duration}"
//	}

//	println "SLOWEST: " + times.max()
//	println "FASTEST: " + times.min()
//	println "AVERAGE: " + times.sum() / times.size()
//}

private runProcess(executablePath) {
	final long startTime = System.currentTimeMillis()
	def sout = new StringBuilder(), serr = new StringBuilder()
	Process proc = executablePath.execute()
	println "After execute: " + (System.currentTimeMillis() - startTime)
	proc.in.withReader { reader ->
		def line = reader.readLine()
		if (line == "First name:") {
			println "Read first name: " + (System.currentTimeMillis() - startTime)
			def firstName = RandomStringUtils.randomAlphabetic(100)
			proc.outputStream.write("$firstName\n".getBytes())
			proc.outputStream.flush()
			println "Wrote first name: " + (System.currentTimeMillis() - startTime)
		}

		line = reader.readLine()
		if (line == "Last name:") {
			println "Read last name: " + (System.currentTimeMillis() - startTime)
			def lastName = RandomStringUtils.randomAlphabetic(100)
			proc.outputStream.write("$lastName\n".getBytes())
			proc.outputStream.flush()
			println "Wrote last name: " + (System.currentTimeMillis() - startTime)
		}
	}

	proc.consumeProcessErrorStream(serr)
	println "Waiting for process to stop: " + (System.currentTimeMillis() - startTime)
	proc.waitFor()
	println "Done: " + (System.currentTimeMillis() - startTime)

	final long endTime = System.currentTimeMillis()

	return [
			output   : sout.toString(),
			error    : serr.toString(),
			exitValue: proc.exitValue(),
			duration: endTime - startTime
	]
}
