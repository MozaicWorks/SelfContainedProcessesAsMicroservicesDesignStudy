import static CommandLineHelpers.*

class CreateUser {
	static void main(String[] args) {
		def cli = new CliBuilder(usage: 'createUser -[create|selfSetup|selfTest|selfCleanup]')
		cli.with {
			help longOpt: 'help', 'show usage information'
			create longOpt: 'create', 'creates a user'
			selfSetup longOpt: 'self-setup', 'creates database and user'
			selfTest longOpt: 'self-test', 'runs self test'
			selfCleanup longOpt: 'self-cleanup', 'drops database and user'
			selfBackup longOpt: 'self-backup', 'backs up the database'
			selfRestore longOpt: 'self-restore', 'restores the last backup'
		}
		def options = cli.parse(args)
		if (!options) {
			cli.usage()
			printError("Pass the correct arguments")
			exitWithError()
		}
		if (!options.create && !options.help && !options.selfSetup &&
				!options.selfCleanup && !options.selfTest &&
				!options.selfBackup && !options.selfRestore) {
			cli.usage()
			printError("Pass the correct arguments")
			exitWithError()
		}
		if (options.help) {
			cli.usage()
			resultOK()
			return
		}
		if (options.selfSetup) {
			CreateUserInterfaceFactory.create().doSelfSetup()
			resultOK()
			return
		}
		if (options.selfTest) {
			CreateUserInterfaceFactory.create().doSelfTest() ? resultOK() : exitWithError()
			return
		}
		if (options.create) {
			CreateUserInterfaceFactory.create().doCreate()
			resultOK()
			return
		}
		if (options.selfCleanup) {
			CreateUserInterfaceFactory.create().doSelfCleanup()
			resultOK()
			return
		}
		if (options.selfBackup) {
			CreateUserInterfaceFactory.create().doSelfBackup()
			resultOK()
			return
		}
		if (options.selfRestore) {
			CreateUserInterfaceFactory.create().doSelfRestore()
			resultOK()
			return
		}
	}
}