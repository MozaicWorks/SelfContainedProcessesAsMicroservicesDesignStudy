#!/usr/bin/env groovyclient
@Grapes([
		@Grab("mysql:mysql-connector-java:5.1.39"),
		@Grab("org.apache.commons:commons-lang3:3.7"),
		@Grab("org.grails:grails-datastore-gorm-hibernate5:6.1.8.RELEASE"),
		@Grab("mysql:mysql-connector-java:5.1.39"),
		@GrabConfig(systemClassLoader = true)
])
import grails.gorm.annotation.Entity
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

final String userSecretsFilename = "user.groovy"
final String adminSecretsFilename = "adminSecrets.groovy"
final String dbName = "Users"

def logger = new MyLogger()

// configure for admin
class DbAdminConfigHolder {
	def dbName
	def logger
	def secretsFilename

	SecretsProvider getSecretsProvider() {
		return new SecretsProviderFromConfigFile(
				fileLocation: new File(secretsFilename).toURL()
		)
	}

	final DatabaseSetup getDatabaseSetup() {
		return new DatabaseSetup(
				databaseOperations:
						new DatabaseOperations(
								adminSqlProvider:
										new MySqlProvider(
												secretsProvider: secretsProvider,
												logger: logger
										),
								dbName: dbName,
								logger: logger
						),
				logger: logger
		)
	}
}

def adminConfigHolder = new DbAdminConfigHolder(secretsFilename: adminSecretsFilename, dbName: dbName, logger: logger)

// configure for user

class DbUserConfigHolder {
	def dbName
	def logger
	SecretsProvider secretsProvider

	DbUserConfigHolder(params) {
		this.dbName = params.dbName
		this.logger = params.logger

		if (params.secretsFilename) {
			this.secretsProvider = secretsProviderFromConfigFile(params.secretsFilename)
		} else {
			this.secretsProvider = params.secretsProvider
		}
	}

	private SecretsProvider getSecretsProvider() {
		this.secretsProvider
	}

	private static SecretsProvider secretsProviderFromConfigFile(secretsFilename) {
		return new SecretsProviderFromConfigFile(
				fileLocation: new File(secretsFilename).toURL()
		) as SecretsProvider
	}

	SqlProvider getSqlProvider() {
		new MySqlProvider(
				dbName: dbName,
				secretsProvider: secretsProvider,
				logger: logger
		)
	}
}

def userConfigHolder = new DbUserConfigHolder(dbName: dbName, secretsFilename: userSecretsFilename, logger: logger)

final CreateUserInterface createUserInterface = new CreateUserInterface(
		userConfigHolder: userConfigHolder,
		adminConfigHolder: adminConfigHolder,
		logger: logger
)

logger.info("START CreateUser")

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
	createUserInterface.doSelfSetup()
}

if (options.selfTest) {
	createUserInterface.doSelfTest() ? resultOK() : exitWithError()
}

if (options.create) {
	createUserInterface.doCreate()
	resultOK()
}

if (options.selfCleanup) {
	createUserInterface.doSelfCleanup()
}

logger.info("END CreateUser")

class CreateUserInterface {

	DbUserConfigHolder userConfigHolder
	DbAdminConfigHolder adminConfigHolder
	def logger

	Boolean doSelfTest() {
		def testDbName = "test" + RandomStringUtils.randomAlphabetic(20)
		def testDbUserName = "test" + RandomStringUtils.randomAlphabetic(20)
		def testDbPassword = "test" + RandomStringUtils.randomAlphabetic(100)
		def userConfigHolder = new DbUserConfigHolder(
				dbName: testDbName,
				secretsProvider: new SecretsProviderInMemory(username: testDbUserName, password: testDbPassword),
				logger: logger
		)
		def adminConfigHolder = new DbAdminConfigHolder(
				dbName: testDbName,
				secretsFilename: adminConfigHolder.secretsFilename,
				logger: logger
		)

		new SelfTest(
				testInstance: new CreateUserInterface(
						userConfigHolder: userConfigHolder,
						adminConfigHolder: adminConfigHolder,
						logger: logger
				),
				productionInstance: this,
				logger: logger
		).run()
	}

	def doCreate() {
		def commandUser = new CommandLineUserMapping()
		def parameterReader = new ParameterReader(scanner: new Scanner(System.in))
		def parameters = parameterReader.readParameters(commandUser.mapping)

		logger.info("Creating user $parameters")
		createUser(parameters)
	}

	def createUser(parameters) {
		doGormMapping()
		User.withTransaction {
			return new User(parameters).save()
		}
	}

	def doSelfSetup() {
		adminConfigHolder.databaseSetup.createDatabaseUserAndSchema(userConfigHolder.secretsProvider) {
			userConfigHolder.sqlProvider.createTable(User.tableName, User.createTable)
		}
	}

	def doSelfCleanup() {
		adminConfigHolder.databaseSetup.dropDatabaseAndUser(
				userConfigHolder.secretsProvider.username
		)
	}


	private doGormMapping() {
		def dbName = userConfigHolder.dbName
		def userSecretsProvider = userConfigHolder.secretsProvider
		Map configuration = [
				'hibernate.hbm2ddl.auto': '',
				'dataSource.url'        : "jdbc:mysql://localhost:3306/$dbName?useSSL=false",
				'dataSource.username'   : userSecretsProvider.username,
				'dataSource.password'   : userSecretsProvider.password,
				'hibernate.dialect'     : 'org.hibernate.dialect.MySQLDialect'
		]
		// need this for GORM mapping
		HibernateDatastore datastore = new HibernateDatastore(configuration, User)
	}
}


class DomainUser {
	String firstName
	String lastName
	Boolean isActive = true
}

@Entity
class User extends DomainUser implements GormEntity<User> {
	final static tableName = 'USER'

	static mapping = {
		table tableName
		version false
		autoTimestamp false
	}

	static createTable = """\
CREATE TABLE `USER`(
	`ID` INT(11) NOT NULL AUTO_INCREMENT,
	`FIRST_NAME` VARCHAR(500) NOT NULL,
	`LAST_NAME` VARCHAR(500) NOT NULL,
	`IS_ACTIVE` BOOLEAN NOT NULL,
	PRIMARY KEY(`ID`)
)"""
}

class CommandLineUserMapping {
	static mapping = [
			firstName: "First Name:",
			lastName : "Last Name:"
	]
}

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
