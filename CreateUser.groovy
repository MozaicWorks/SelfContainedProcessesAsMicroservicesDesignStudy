@Grab("mysql:mysql-connector-java:5.1.39")
@Grab("org.apache.commons:commons-lang3:3.7")
@GrabConfig(systemClassLoader = true)

import groovy.sql.Sql
import groovy.transform.ToString
import org.apache.commons.lang3.RandomStringUtils

import java.util.logging.Logger
import java.util.logging.SimpleFormatter

Logger logger = Logger.getLogger("CreateUser")
logger.info("START CreateUser")

def normalUserSecretsProvider = [username: "user", password: "password"]
def adminSecretsProvider = [username: "root", password: ""]
def dbName = "Users"

def firstName = "A"
def lastName = "B"

CreateUserParameters createUserParameters = new CreateUserParameters(firstName: firstName, lastName: lastName)

def adminSqlProvider = new MySqlProvider(secretsProvider: adminSecretsProvider)
DatabaseOperations databaseOperations = new DatabaseOperations(
		adminSqlProvider: adminSqlProvider, dbName: dbName, logger: logger
)
def normalUserSqlProvider = new MySqlProvider(dbName: dbName, secretsProvider: normalUserSecretsProvider)

selfSetup(databaseOperations, normalUserSecretsProvider, logger)
selfTest(normalUserSqlProvider, logger)

createUser(
		createUserParameters,
		new Command(sqlProvider: normalUserSqlProvider, logger: logger),
		logger
)

selfCleanup(databaseOperations, normalUserSecretsProvider, logger)


static def createUser(CreateUserParameters parameters, Command command, logger) {
	logger.info("Creating user $parameters")
	def isActive = parameters.formatIsActiveForDb()

	return command.executeSqlCommand(
			"""\
		INSERT INTO USER(FIRST_NAME, LAST_NAME, IS_ACTIVE) 
		VALUES($parameters.firstName, $parameters.lastName, $isActive)
		""",
			"createUser", 1
	)
}

static def selfSetup(DatabaseOperations databaseOperations, userSecretsProvider, logger) {
	logger.info("START self setup")
	databaseOperations.createDatabase()
	databaseOperations.createUser(userSecretsProvider)

	MySqlProvider userSqlProvider = new MySqlProvider(
			dbName: databaseOperations.dbName,
			secretsProvider: userSecretsProvider,
	)

	def command = new Command(sqlProvider: userSqlProvider, logger: logger)
	createTable(command, logger)

	logger.info("END self setup")
}

static def existsTable(sqlProvider) {
	def rows = sqlProvider.firstRow(
			"""
	SHOW TABLES LIKE 'USER';
""")
	return (rows?.size() == 1)
}

static def selfCleanup(DatabaseOperations databaseOperations, userSecretsProvider, logger) {
	logger.info("START self cleanup")
	databaseOperations.dropDatabase()

	def username = userSecretsProvider.username
	databaseOperations.dropUser(username)

	logger.info("END self cleanup")
}

static def createTable(command, logger) {
	if (!existsTable(command.sqlProvider)) {
		command.executeSqlCommand(
				"""CREATE TABLE `USER`(
		                `ID` INT(11) NOT NULL AUTO_INCREMENT,
		                `FIRST_NAME` VARCHAR(500) NOT NULL,
		                `LAST_NAME` VARCHAR(500) NOT NULL,
		                `IS_ACTIVE` BOOLEAN NOT NULL,
		                PRIMARY KEY(`ID`)
		        )""", "createTableUser", 1
		)
	} else {
		logger.info("Table USER exists; nothing to do")
	}
}

static def selfTest(sqlProvider, logger) {
	logger.info("START SELF-TEST")
	def firstName = RandomStringUtils.randomAlphabetic(100)
	def lastName = RandomStringUtils.randomAlphabetic(100)
	def parameters = new CreateUserParameters(firstName: firstName, lastName: lastName, isActive: false)
	def command = new Command(logger: logger, sqlProvider: sqlProvider)

	try {
		testCreateUser(parameters, command, logger)
		logger.info("Self test successful")
	} finally {
		def result = deleteTestUser(parameters, command)
		if (result.isSuccessful()) {
			logger.info("Clean up successful")
		} else {
			logger.severe("Clean up error. Can't recover, will stop")
		}
	}
	logger.info("END SELF-TEST")
}

private static deleteTestUser(CreateUserParameters parameters, Command command) {
	def firstName = parameters.firstName
	def lastName = parameters.lastName
	def commandString = """
		DELETE FROM USER 
		WHERE FIRST_NAME=$firstName 
		AND LAST_NAME=$lastName 
		AND NOT IS_ACTIVE
"""
	return command.executeSqlCommand(commandString, "deleteTestUser", 1)
}

private static assertUserWasCreated(sqlProvider, firstName, lastName) {
	def rows = sqlProvider.firstRow(
			"""
		SELECT COUNT(ID) AS count FROM USER WHERE FIRST_NAME=${firstName} AND LAST_NAME=${lastName}
""")
	assert rows.count == 1, "Cannot retrieve the created user"
}

private static testCreateUser(CreateUserParameters parameters, Command command, logger) {
	def result = createUser(parameters, command, logger)

	assert result.isSuccessful(), "Could not create a user with $parameters"
	assertUserWasCreated(command.sqlProvider, parameters.firstName, parameters.lastName)
}

@ToString(includePackage = false, includeFields = true, includeNames = true)
class CreateUserParameters {
	def firstName
	def lastName
	boolean isActive = true

	def formatIsActiveForDb() {
		return isActive ? 1 : 0
	}
}

logger.info("END CreateUser")
