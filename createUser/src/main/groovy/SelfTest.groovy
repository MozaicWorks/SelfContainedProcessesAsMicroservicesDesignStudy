import org.apache.commons.lang3.RandomStringUtils

class SelfTest {
	CreateUserInterface productionInstance
	CreateUserInterface testInstance

	private user
	def logger

	def run() {
		try {
			logger.info("START SELF-TEST")
			def createUserInterface = testInstance
			createUserInterface.doSelfSetup()
			testCreate(createUserInterface)
			createUserInterface.doSelfCleanup()
			logger.info("SUCCESS SELF-TEST")
			return true
		} catch (Exception exc) {
			logger.severe "FAILED SELF-TEST: exception ${exc.toString()}"
			return false
		} finally {
			logger.info("END SELF-TEST")
		}
	}

	def testCreate(createUserInterface) {
		testCreateUser(createUserInterface)
		deleteUser()
	}

	private deleteUser() {
		User.withTransaction {
			user.delete()
		}
		logger.info("SelfTest: Clean up successful")
	}

	static def assertUserWasCreated(firstName, lastName) {
		User.withTransaction {
			assert User.findAllByFirstNameAndLastName(firstName, lastName).size() == 1, "SelfTest: could not find created user"
		}
	}

	def testCreateUser(CreateUserInterface createUserInterface) {
		def firstName = RandomStringUtils.randomAlphabetic(100)
		def lastName = RandomStringUtils.randomAlphabetic(100)
		def parameters = [firstName: firstName, lastName: lastName, isActive: false]

		user = createUserInterface.createUser(parameters)

		assert user, "SelfTest: Could not create a user with $parameters"
		assertUserWasCreated(parameters.firstName, parameters.lastName)
	}
}
