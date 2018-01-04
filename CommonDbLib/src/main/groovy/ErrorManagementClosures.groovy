class ErrorManagementClosures {
	static def tryCatchAll(Closure action, Closure exceptionReaction) {
		try {
			action()
		} catch (Exception ex) {
			exceptionReaction(ex)
		}
	}
}
