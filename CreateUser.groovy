@Grab("mysql:mysql-connector-java:5.1.39")
@Grab("org.apache.commons:commons-lang3:3.7")
@GrabConfig(systemClassLoader = true)

import groovy.sql.Sql
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.apache.commons.lang3.RandomStringUtils
import java.util.logging.Logger

def createUserParameters = new CreateUserParameters(firstName: "A", lastName: "B")

Logger logger = Logger.getLogger("CreateUser")
def sql = Sql.newInstance(
		'jdbc:mysql://localhost:3306/Users',
		'user',
		'password',
		'com.mysql.jdbc.Driver'
)
sql.connection.autoCommit = false

selfTest(sql, logger)

def result = createUser(createUserParameters, sql, logger)
result.log(logger)

def createUser(CreateUserParameters parameters, Sql sql, def logger) {
	logger.info("Creating user $parameters")
	def firstName = parameters.firstName
	def lastName = parameters.lastName

	def commandString = """ 
		INSERT INTO 
        USER(FIRST_NAME, LAST_NAME) 
        VALUES($firstName, $lastName)
"""
	return executeSqlCommand(commandString, "createUser", sql, logger)
}


def createTable(Sql sql, def logger) {
	def commandString = """
			CREATE TABLE `USER`(
                `ID` INT(11) NOT NULL AUTO_INCREMENT,
                `FIRST_NAME` VARCHAR(500) NOT NULL,
                `LAST_NAME` VARCHAR(500) NOT NULL,
                PRIMARY KEY(`ID`)
                )
    """

	return executeSqlCommand(commandString, "createTable", sql, logger)
}

def selfTest(sql, logger) {
	def firstName = RandomStringUtils.randomAlphabetic(500)
	def lastName = RandomStringUtils.randomAlphabetic(500)
	def parameters = new CreateUserParameters(firstName: firstName, lastName: lastName)

	try {
		testCreateUser(parameters, sql, logger)
	} finally {
		deleteTestUser(parameters, sql, logger)
	}
}

private deleteTestUser(CreateUserParameters parameters, sql, logger) {
	def commandString = """
		DELETE FROM USER WHERE FIRST_NAME=$parameters.firstName AND LAST_NAME=$parameters.lastName
"""
	def result = executeSqlCommand(commandString, "deleteTestUser", sql, logger)

	assert result.isSuccessful()
	assert sql.updateCount == 1, "Could not clean up; will exit now"
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


private Result executeSqlCommand(commandString, action, Sql sql, logger) {
	def result
	try {
		sql.execute(commandString)
		sql.commit()
		result = Result.success(action)
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