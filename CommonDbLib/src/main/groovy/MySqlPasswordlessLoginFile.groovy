class MySqlPasswordlessLoginFile {
	SecretsProvider secretsProvider

	def createForDump() {
		def configFile = new File(System.getProperty("user.home"), "/.my.cnf")
		configFile.createNewFile()
		configFile.write("""\
[mysqldump]
password=${secretsProvider.password}
""")
	}

	def createForClient() {
		def configFile = new File(System.getProperty("user.home"), "/.my.cnf")
		configFile.createNewFile()
		configFile.write("""\
[client]
password=${secretsProvider.password}
""")
	}

	def delete() {
		new File(System.getProperty("user.home"), "/.my.cnf").delete()
	}
}
