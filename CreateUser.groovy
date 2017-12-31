@Grab("mysql:mysql-connector-java:5.1.39")
@Grab("org.apache.commons:commons-lang3:3.7")
@GrabConfig(systemClassLoader = true)

import groovy.sql.Sql
import groovy.transform.ToString
import org.apache.commons.lang3.RandomStringUtils

import java.util.logging.Logger

def normalUserSecretsProvider = [username: "user", password: "password"]
def adminSecretsProvider = [username: "root", password: ""]
def dbName = "Users"

def firstName = "A"
def lastName = "B"

CreateUserParameters createUserParameters = new CreateUserParameters(firstName: firstName, lastName: lastName)
Logger logger = Logger.getLogger("CreateUser")

Sql adminSql = new MySqlBuilder(
		secretsProvider: adminSecretsProvider
).getSql()

MySqlBuilder normalUserSqlBuilder = new MySqlBuilder(
		dbName: dbName,
		secretsProvider: normalUserSecretsProvider
)

DatabaseOperations databaseOperations = new DatabaseOperations(adminSql: adminSql, dbName: dbName, logger: logger)
selfSetup(databaseOperations, normalUserSecretsProvider, logger)

Sql normalUserSql = normalUserSqlBuilder.getSql()

selfTest(normalUserSql, logger)

def result = createUser(createUserParameters, normalUserSql, logger)
result.log(logger)

selfCleanup(databaseOperations, normalUserSecretsProvider)

adminSql.close()
normalUserSql.close()

static def createUser(CreateUserParameters parameters, Sql sql, def logger) {
	logger.info("Creating user $parameters")
	def firstName = parameters.firstName
	def lastName = parameters.lastName
	def isActive = parameters.formatIsActiveForDb()

	GString commandString = """ 
		INSERT INTO USER(FIRST_NAME, LAST_NAME, IS_ACTIVE) 
        VALUES($firstName, $lastName, $isActive)
"""
	return new Command().executeSqlCommand(commandString, "createUser", 1, sql, logger)
}

static def selfSetup(DatabaseOperations databaseOperations, userSecretsProvider, logger) {
	databaseOperations.createDatabase()
	databaseOperations.createUser(userSecretsProvider)

	MySqlBuilder userSqlBuilder = new MySqlBuilder(
			dbName: databaseOperations.dbName,
			secretsProvider: userSecretsProvider,
	)
	createTable(userSqlBuilder.getSql(), logger)
}

static def selfCleanup(DatabaseOperations databaseOperations, userSecretsProvider) {
	databaseOperations.dropDatabase()

	def username = userSecretsProvider.username
	databaseOperations.dropUser(username)
}

static def createTable(Sql sql, def logger) {
	def commandString = """
			CREATE TABLE `USER`(
                `ID` INT(11) NOT NULL AUTO_INCREMENT,
                `FIRST_NAME` VARCHAR(500) NOT NULL,
                `LAST_NAME` VARCHAR(500) NOT NULL,
                `IS_ACTIVE` BOOLEAN NOT NULL,
                PRIMARY KEY(`ID`)
                )
    """

	def result = new Command().executeSqlCommand(commandString, "createTableUser", 1, sql, logger)
	result.log(logger)
}

def selfTest(sql, logger) {
	logger.info("START SELF-TEST")
	def firstName = RandomStringUtils.randomAlphabetic(100)
	def lastName = RandomStringUtils.randomAlphabetic(100)
	def parameters = new CreateUserParameters(firstName: firstName, lastName: lastName, isActive: false)

	try {
		testCreateUser(parameters, sql, logger)
		logger.info("Self test successful")
	} finally {
		def result = deleteTestUser(parameters, sql, logger)
		if (result.isSuccessful()) {
			logger.info("Clean up successful")
		} else {
			logger.severe("Clean up error. Can't recover, will stop")
		}
	}
	logger.info("END SELF-TEST")
}

private static deleteTestUser(CreateUserParameters parameters, sql, logger) {
	def firstName = parameters.firstName
	def lastName = parameters.lastName
	def commandString = """
		DELETE FROM USER 
		WHERE FIRST_NAME=$firstName 
		AND LAST_NAME=$lastName 
		AND NOT IS_ACTIVE
"""
	return new Command().executeSqlCommand(commandString, "deleteTestUser", 1, sql, logger)
}

private static assertUserWasCreated(sql, firstName, lastName) {
	def rows = sql.firstRow(
			"""
		SELECT COUNT(ID) AS count FROM USER WHERE FIRST_NAME=${firstName} AND LAST_NAME=${lastName}
""")
	assert rows.count == 1, "Cannot retrieve the created user"
}

private static testCreateUser(CreateUserParameters parameters, Sql sql, logger) {
	def result = createUser(parameters, sql, logger)

	assert result.isSuccessful(), "Could not create a user with $parameters"
	assertUserWasCreated(sql, parameters.firstName, parameters.lastName)
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
