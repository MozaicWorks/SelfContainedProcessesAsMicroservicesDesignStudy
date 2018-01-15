import grails.gorm.annotation.Entity
import org.grails.datastore.gorm.GormEntity

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
