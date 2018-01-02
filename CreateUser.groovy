#!/usr/bin/env groovyclient

@Grapes([
		@Grab("mysql:mysql-connector-java:5.1.39"),
		@Grab("org.apache.commons:commons-lang3:3.7"),
		@Grab("org.grails:grails-datastore-gorm-hibernate5:6.1.8.RELEASE"),
		@Grab("mysql:mysql-connector-java:5.1.39"),
		@GrabConfig(systemClassLoader = true)
])

import grails.gorm.annotation.Entity
import grails.persistence.*
import org.apache.commons.lang3.RandomStringUtils
import org.grails.datastore.gorm.GormEntity
import org.grails.orm.hibernate.HibernateDatastore
import grails.gorm.annotation.Entity

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

def normalUserSecretsProvider = new UserSecretsProviderFromConfigFile(fileLocation: new File(userSecretsFileName).toURL())

Map configuration = [
		'hibernate.hbm2ddl.auto': '',
		'dataSource.url'        : "jdbc:mysql://localhost:3306/$dbName?useSSL=false",
		'dataSource.username'   : normalUserSecretsProvider.username,
		'dataSource.password'   : normalUserSecretsProvider.password,
		'hibernate.dialect'     : 'org.hibernate.dialect.MySQLDialect'
]
// need this for GORM mapping
HibernateDatastore datastore = new HibernateDatastore(configuration, User)

if (options.selfSetup) {
	def adminSecretsProvider = new UserSecretsProviderFromConfigFile(fileLocation: new File(adminSecretsFilename).toURL())
	def adminSqlProvider = new MySqlProvider(secretsProvider: adminSecretsProvider)
	DatabaseOperations databaseOperations = new DatabaseOperations(
			adminSqlProvider: adminSqlProvider, dbName: dbName, logger: logger
	)

	doSelfSetup(databaseOperations, normalUserSecretsProvider, logger)
}

if (options.selfTest) {
	doSelfTest(logger)
}

if (options.create) {
	doCreate(logger)
	resultOK()
}

if (options.selfCleanup) {
	def adminSecretsProvider = new UserSecretsProviderFromConfigFile(fileLocation: new File(adminSecretsFilename).toURL())
	def adminSqlProvider = new MySqlProvider(secretsProvider: adminSecretsProvider)
	DatabaseOperations databaseOperations = new DatabaseOperations(
			adminSqlProvider: adminSqlProvider, dbName: dbName, logger: logger
	)

	doSelfCleanup(databaseOperations, normalUserSecretsProvider, logger)
}

private static doCreate(logger) {
	def scanner = new Scanner(System.in)

	def commandUser = new CommandLineUserMapping()
	def parameters = commandUser.mapping.collectEntries { columnName, labelName ->
		println labelName
		def value = scanner.nextLine()
		return ["$columnName": value]
	}

	createUser(parameters, logger)
}

static def createUser(parameters, logger) {
	logger.info("Creating user $parameters")

	User.withTransaction {
		def result = new User(parameters).save()
		return result != null
	}
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

static def doSelfTest(logger) {
	logger.info("START SELF-TEST")
	def firstName = RandomStringUtils.randomAlphabetic(100)
	def lastName = RandomStringUtils.randomAlphabetic(100)
	def parameters = [firstName: firstName, lastName: lastName, isActive: false]

	try {
		testCreateUser(parameters, logger)
		logger.info("SUCCESS SELF-TEST")
	} finally {
		try {
			deleteTestUser(parameters)
			logger.info("Clean up successful")
		} catch (Exception exc) {
			logger.severe("Clean up error $exc.message. Can't recover, will stop")
		}
	}
	logger.info("END SELF-TEST")
}

private static deleteTestUser(parameters) {
	User.withTransaction {
		def user = User.findByFirstNameAndLastNameAndIsActive(parameters.firstName, parameters.lastName, false)
		user.delete()
	}
}

private static assertUserWasCreated(firstName, lastName) {
	User.withTransaction {
		assert User.findAllByFirstNameAndLastName(firstName, lastName).size() == 1
	}
}

private static testCreateUser(parameters, logger) {
	def result = createUser(parameters, logger)

	assert result, "Could not create a user with $parameters"
	assertUserWasCreated(parameters.firstName, parameters.lastName)
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

class DomainUser {
	String firstName
	String lastName
	Boolean isActive = true
}

@Entity
class User extends DomainUser implements GormEntity<User> {
	static mapping = {
		table 'USER'
		version false
		autoTimestamp false
	}
}

class CommandLineUserMapping{
	static mapping = [
			firstName: "First Name:",
			lastName : "Last Name:"
	]
}
