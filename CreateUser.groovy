#!/usr/bin/env groovy

@Grab("org.codehaus.gpars:gpars:1.2.0")
@Grab("mysql:mysql-connector-java:5.1.39")
@Grab("org.apache.commons:commons-lang3:3.7")
@GrabConfig(systemClassLoader = true)

import groovy.sql.Sql
import groovy.transform.ToString
import org.apache.commons.lang3.RandomStringUtils
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import groovyx.gpars.GParsExecutorsPool

def logger = new MyLogger()
logger.info("START CreateUser")

final def userSecretsFileName = "user.groovy"
final def adminSecretsFilename = "adminSecrets.groovy"
final def dbName = "Users"

def cli = new CliBuilder(usage: 'createUser -[create|selfSetup|selfTest|selfCleanup]')
cli.with {
	help longOpt: 'help', 'show usage information'
	create longOpt: 'create', 'creates a user'
	selfSetup longOpt: 'self setup', 'creates database and user'
	selfTest longOpt: 'self test', 'runs self test'
	selfCleanup longOpt: 'self cleanup', 'drops database and user'
	// need a self backup
}

def options = cli.parse(args)
if (!options) {
	cli.usage()
	printError("Pass the correct arguments")
	exitWithError()
}

if (!options.create && !options.help && !options.selfSetup && !options.selfCleanup && !options.selfTest) {
	cli.usage()
	printError("Pass the correct arguments")
	exitWithError()
}

void printError(message) {
	println "ERROR: $message"
}

private static exitWithError() {
	throw new RuntimeException()
}

private static resultOK() {
	println "OK"
}

if (options.help) {
	cli.usage()
	resultOK()
	return
}

if (options.selfSetup) {
	def normalUserSecretsProvider = new UserSecretsProviderFromConfigFile(fileLocation: new File(userSecretsFileName).toURL())
	def adminSecretsProvider = new UserSecretsProviderFromConfigFile(fileLocation: new File(adminSecretsFilename).toURL())
	def adminSqlProvider = new MySqlProvider(secretsProvider: adminSecretsProvider)
	DatabaseOperations databaseOperations = new DatabaseOperations(
			adminSqlProvider: adminSqlProvider, dbName: dbName, logger: logger
	)

	doSelfSetup(databaseOperations, normalUserSecretsProvider, logger)
}

if (options.selfTest) {
	def normalUserSecretsProvider = new UserSecretsProviderFromConfigFile(fileLocation: new File(userSecretsFileName).toURL())
	def normalUserSqlProvider = new MySqlProvider(dbName: dbName, secretsProvider: normalUserSecretsProvider)

	doSelfTest(normalUserSqlProvider, logger)
}

if (options.create) {
	def normalUserSecretsProvider = new UserSecretsProviderFromConfigFile(fileLocation: new File(userSecretsFileName).toURL())

	doCreate(normalUserSecretsProvider, dbName, logger)
}

private static doCreate(userSecretsProvider, String dbName, MyLogger logger) {
	def scanner = new Scanner(System.in)
	println("First name:")
	def firstName = scanner.nextLine()
	println("Last name:")
	def lastName = scanner.nextLine()
	CreateUserParameters createUserParameters = new CreateUserParameters(firstName: firstName, lastName: lastName)
	def normalUserSqlProvider = new MySqlProvider(dbName: dbName, secretsProvider: userSecretsProvider)
	createUser(
			createUserParameters,
			new Command(sqlProvider: normalUserSqlProvider, logger: logger),
			logger
	)
}

if (options.selfCleanup) {
	def normalUserSecretsProvider = new UserSecretsProviderFromConfigFile(fileLocation: new File(userSecretsFileName).toURL())
	def adminSecretsProvider = new UserSecretsProviderFromConfigFile(fileLocation: new File(adminSecretsFilename).toURL())
	def adminSqlProvider = new MySqlProvider(secretsProvider: adminSecretsProvider)
	DatabaseOperations databaseOperations = new DatabaseOperations(
			adminSqlProvider: adminSqlProvider, dbName: dbName, logger: logger
	)

	doSelfCleanup(databaseOperations, normalUserSecretsProvider, logger)
}

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

static def doSelfSetup(DatabaseOperations databaseOperations, userSecretsProvider, logger) {
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

static def doSelfCleanup(DatabaseOperations databaseOperations, userSecretsProvider, logger) {
	logger.info("START self cleanup")

	databaseOperations.dropDatabase()
	databaseOperations.dropUser(userSecretsProvider.username)

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

static def doSelfTest(sqlProvider, logger) {
	logger.info("START SELF-TEST")
	def firstName = RandomStringUtils.randomAlphabetic(100)
	def lastName = RandomStringUtils.randomAlphabetic(100)
	def parameters = new CreateUserParameters(firstName: firstName, lastName: lastName, isActive: false)
	def command = new Command(logger: logger, sqlProvider: sqlProvider)

	try {
		testCreateUser(parameters, command, logger)
		logger.info("SUCCESS SELF-TEST")
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


class UserSecretsProviderFromConfigFile {
	def config = new ConfigSlurper()
	URL fileLocation

	def getUsername() {
		def secrets = config.parse(fileLocation)
		return secrets.username
	}

	def getPassword() {
		def secrets = config.parse(fileLocation)
		return secrets.password
	}
}

class MyLogger {

	def info(message) {
		log("INFO", message)
	}

	def severe(message) {
		log("SEVERE", message)
	}

	private log(prefix, message) {
		def date = new Date().format("DD/MM/YYYY HH:mm:ss")

		System.err.println "$date $prefix: $message"
	}
}