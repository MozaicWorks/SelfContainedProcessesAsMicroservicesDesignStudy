import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class Result {
	int code
	String action
	String message

	static fromException(Exception exc, String action) {
		return new Result(code: -1, action: action, message: exc.toString())
	}

	static success(String action) {
		return new Result(code: 0, action: action, message: "OK")
	}

	static wrongUpdateCount(String action, int updateCount, int actualUpdateCount) {
		return new Result(code: -1, action: action,
				message: "Wrong update count - expected $updateCount and got $actualUpdateCount"
		)
	}

	def isSuccessful() {
		return code == 0 && message == "OK"
	}

	def log(logger) {
		if (this.isSuccessful())
			logger.info("SUCCESS $action")
		else
			logger.severe("ERROR when $action: $message")
	}
}
