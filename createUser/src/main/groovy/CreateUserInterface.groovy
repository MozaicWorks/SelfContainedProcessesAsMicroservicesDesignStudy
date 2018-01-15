import org.apache.commons.lang3.RandomStringUtils
import org.grails.orm.hibernate.HibernateDatastore

class CreateUserInterface {
	DbUserConfigHolder userConfigHolder
	DbAdminConfigHolder adminConfigHolder
	def logger

	Boolean doSelfTest() {
		def testDbName = "test" + RandomStringUtils.randomAlphabetic(20)
		def testDbUserName = "test" + RandomStringUtils.randomAlphabetic(20)
		def testDbPassword = "test" + RandomStringUtils.randomAlphabetic(100)
		def adminSecretsFileName = adminConfigHolder.secretsFilename
		def userSecretsProvider = new SecretsProviderInMemory(username: testDbUserName, password: testDbPassword)

		CreateUserInterface testInstance = CreateUserInterfaceFactory.createWithUserSecretsProvider(
				testDbName, userSecretsProvider, adminSecretsFileName, logger
		)

		new SelfTest(
				testInstance: testInstance,
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

	def doSelfBackup() {
		adminConfigHolder.databaseSetup.backup(userConfigHolder.secretsProvider, userConfigHolder.dbName)
	}

	def doSelfRestore() {
		adminConfigHolder.databaseSetup.restore(userConfigHolder.secretsProvider, userConfigHolder.dbName)
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
