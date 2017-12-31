@Grab("mysql:mysql-connector-java:5.1.39")
@Grab("org.apache.commons:commons-lang3:3.7")
@GrabConfig(systemClassLoader = true)

import groovy.sql.Sql
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.apache.commons.lang3.RandomStringUtils
import java.util.logging.Logger

def normalUserSecretsProvider = [username: "user", password: "password"]
def adminSecretsProvider = [username: "root", password: ""]
def dbName = "Users"

def firstName = "A"
def lastName = "B"

def createUserParameters = new CreateUserParameters(firstName: firstName, lastName: lastName)
Logger logger = Logger.getLogger("CreateUser")

def adminSql = new MySqlBuilder(
		secretsProvider: adminSecretsProvider
).getSql()

def normalUserSqlBuilder = new MySqlBuilder(
		dbName: dbName,
		secretsProvider: normalUserSecretsProvider
)

def databaseOperations = new DatabaseOperations(adminSql: adminSql, dbName: dbName, logger: logger)
selfSetup(databaseOperations, normalUserSecretsProvider, logger)

def normalUserSql = normalUserSqlBuilder.getSql()

selfTest(normalUserSql, logger)

def result = createUser(createUserParameters, normalUserSql, logger)
result.log(logger)

selfCleanup(databaseOperations, normalUserSecretsProvider)

def createUser(CreateUserParameters parameters, Sql sql, def logger) {
	logger.info("Creating user $parameters")
	def firstName = parameters.firstName
	def lastName = parameters.lastName
	def isActive = parameters.formatIsActiveForDb()

	def commandString = """ 
		INSERT INTO 
        USER(FIRST_NAME, LAST_NAME, IS_ACTIVE) 
        VALUES($firstName, $lastName, $isActive)
"""
	return executeSqlCommand(commandString, "createUser", 1, sql, logger)
}

def selfSetup(DatabaseOperations databaseOperations, userSecretsProvider, logger) {
	databaseOperations.createDatabase(userSecretsProvider)
	databaseOperations.createUser(userSecretsProvider)

	def userSqlBuilder = new MySqlBuilder(
			dbName: databaseOperations.dbName,
			secretsProvider: userSecretsProvider,
	)
	createTable(userSqlBuilder.getSql(), logger)
}

def selfCleanup(databaseOperations, userSecretsProvider) {
	databaseOperations.dropDatabase()

	def username = userSecretsProvider.username
	databaseOperations.dropUser(username)
}

def createTable(Sql sql, def logger) {
	def commandString = """
			CREATE TABLE `USER`(
                `ID` INT(11) NOT NULL AUTO_INCREMENT,
                `FIRST_NAME` VARCHAR(500) NOT NULL,
                `LAST_NAME` VARCHAR(500) NOT NULL,
                `IS_ACTIVE` BOOLEAN NOT NULL,
                PRIMARY KEY(`ID`)
                )
    """

	def result = executeSqlCommand(commandString, "createTableUser", 1, sql, logger)
	result.log(logger)
}

def selfTest(sql, logger) {
	def firstName = RandomStringUtils.randomAlphabetic(500)
	def lastName = RandomStringUtils.randomAlphabetic(500)
	def parameters = new CreateUserParameters(firstName: firstName, lastName: lastName, isActive: false)

	try {
		testCreateUser(parameters, sql, logger)
		logger.info("Self test successful")
	} finally {
		deleteTestUser(parameters, sql, logger)
		logger.info("Clean up successful")
	}
}

private deleteTestUser(CreateUserParameters parameters, sql, logger) {
	def commandString = """
		DELETE FROM USER 
		WHERE FIRST_NAME=$parameters.firstName 
		AND LAST_NAME=$parameters.lastName 
		AND NOT IS_ACTIVE
"""
	def result = executeSqlCommand(commandString, "deleteTestUser", 1, sql, logger)
	assert result.isSuccessful()
}

private assertUserWasCreated(sql, firstName, lastName) {
	def rows = sql.firstRow(
			"""
		SELECT COUNT(ID) AS count FROM USER WHERE FIRST_NAME=${firstName} AND LAST_NAME=${lastName}
""")
	assert rows.count == 1, "Cannot retrieve the created user"
}

private testCreateUser(parameters, sql, logger) {
	def result = createUser(parameters, sql, logger)

	assert result.isSuccessful(), "Could not create a user with firstName $parameters"
	assertUserWasCreated(sql, parameters.firstName, parameters.lastName)
}

private Result executeSqlCommand(String commandString, String action, int expectedUpdateCount, Sql sql, logger) {
	def result
	try {
		sql.execute(commandString)
		sql.commit()

		def actualUpdateCount = sql.updateCount
		if (expectedUpdateCount == actualUpdateCount) {
			result = Result.success(action)
		} else {
			result = Result.wrongUpdateCount(action, expectedUpdateCount, actualUpdateCount)
		}
	} catch (Exception ex) {
		sql.rollback()
		result = Result.fromException(ex, action)
	}
	result.log(logger)
	return result
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

class MySqlBuilder {
	String baseUrl = "jdbc:mysql://localhost:3306"
	String dbName
	def secretsProvider
	String driver = "com.mysql.jdbc.Driver"

	def getSql() {
		def sql = Sql.newInstance(
				dbUrl(),
				secretsProvider.username,
				secretsProvider.password,
				driver
		)
		sql.connection.autoCommit = false
		return sql
	}

	def dbUrl() {
		return this.dbName ? "$baseUrl/${this.dbName}" : baseUrl
	}

}

class DatabaseOperations {
	private adminSql
	private dbName
	private logger

	def createDatabase() {
		if (!databaseExists()) {
			adminSql.executeUpdate("CREATE DATABASE " + this.dbName)
			logger.info("SUCCESS: create database ${this.dbName}")
		} else {
			logger.info("Nothing to setup for database $dbName")
		}
	}

	def databaseExists() {
		def row = adminSql.firstRow("""
	SELECT SCHEMA_NAME as schemaName FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = $this.dbName
"""
		)
		return (row?.schemaName == dbName)
	}

	def dropDatabase() {
		if (databaseOperations.databaseExists()) {
			adminSql.executeUpdate("DROP DATABASE " + dbName)
			logger.info("SUCCESS: dropped database $dbName")
		} else {
			logger.info("Database $dbName doesn't exist")
		}
	}

	def userExists(username) {
		def row = adminSql.firstRow("""
	SELECT * FROM mysql.user WHERE user = $username
""")
		return (row?.user == username)
	}

	def createUser(userSecretsProvider) {
		def username = userSecretsProvider.username
		def password = userSecretsProvider.password

		if (!userExists(username)) {
			adminSql.executeUpdate("GRANT USAGE ON *.* TO " + username + "@localhost IDENTIFIED BY $password")
			adminSql.executeUpdate("GRANT ALL PRIVILEGES ON " + dbName + ".* TO $username@localhost")
			adminSql.executeUpdate("FLUSH PRIVILEGES")
			logger.info("SUCCESS: created user $username")
		} else {
			logger.info("Nothing to setup for user $username")
		}
	}

	def dropUser(username) {
		if (userExists(username)) {
			this.adminSql.executeUpdate("DROP USER $username@localhost")
			this.logger.info("SUCCESS: dropped user $username")
		} else {
			logger.info("User $username doesn't exist. Nothing left to do")
		}
	}
}