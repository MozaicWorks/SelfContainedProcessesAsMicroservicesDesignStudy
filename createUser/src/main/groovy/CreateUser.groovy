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
			exitWithError("Pass the correct arguments")
		}
		if (!options.create && !options.help && !options.selfSetup &&
				!options.selfCleanup && !options.selfTest &&
				!options.selfBackup && !options.selfRestore) {
			cli.usage()
			exitWithError("Pass the correct arguments")
		}
		if (options.help) {
			cli.usage()
			exitWithSuccess()
		}
		if (options.selfSetup) {
			CreateUserInterfaceFactory.create().doSelfSetup()
			exitWithSuccess()
		}
		if (options.selfTest) {
			CreateUserInterfaceFactory.create().doSelfTest() ? exitWithSuccess() : exitWithError()
		}
		if (options.create) {
			CreateUserInterfaceFactory.create().doCreate()
			exitWithSuccess()
		}
		if (options.selfCleanup) {
			CreateUserInterfaceFactory.create().doSelfCleanup()
			exitWithSuccess()
		}
		if (options.selfBackup) {
			CreateUserInterfaceFactory.create().doSelfBackup()
			exitWithSuccess()
		}
		if (options.selfRestore) {
			CreateUserInterfaceFactory.create().doSelfRestore()
			exitWithSuccess()
		}
	}
}