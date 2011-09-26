package home.ingvar.passbook.dao;

import home.ingvar.passbook.dao.h2.H2DaoFactory;

/**
 * Класс разделяющий уровень приложения и уровень данных
 * 
 * База данных должна включать в себя следующие обязательные данные:
 * Schema:
 *     - passbook
 * Tables:
 *     - users
 *         > id long auto increment primary key
 *         > username varchar(20) not null unique
 *         > password char(64) not null - contains hash sha256(password + username)
 *         > fullname varchar(40)
 *     - items
 *         > owner_id long references (users.id) not null
 *         > service varchar(100) not null
 *         > username varchar(100) not null
 *         > password varchar(100)
 *         > comment varchar(200)
 *         > primary key(owner_id, service, username)
 *         
 * Все данные в таблице items, кроме owner_id необходимо шифровать используя пароль пользователя(именно пароль, а не его хэш)
 * 
 * @author ingvar
 * @version 0.3
 *
 */
public abstract class DaoFactory {
	
	/**
	 * H2 database (http://www.h2database.com)
	 */
	public static final int H2 = 1;
	
	/**
	 * Create DaoFactory instance
	 * 
	 * @param type DaoFactory type. (e.q. DaoFactory.<b>H2</b>)
	 * @return new instance
	 * @throws InstantiationException if type not defined
	 */
	public static DaoFactory newInstance(int type) throws InstantiationException {
		switch(type) {
			case H2: return new H2DaoFactory();
		}
		throw new InstantiationException("Instance not defined");
	}
	
	/**
	 * Return UserDAO object for manipulating this <i>User</i> data
	 * 
	 * @return an UserDAO object
	 */
	public abstract UserDAO getUserDAO();
	/**
	 * Return ItemDAO object for manipulating this <i>Item</i> data
	 * 
	 * @return an ItemDAO object
	 */
	public abstract ItemDAO getItemDAO();
	/**
	 * Test connect to database
	 */
	public abstract boolean test();
	/**
	 * Create new data storage and initialize its structure
	 * 
	 * @throws ResultException if something wrong
	 */
	public abstract void install() throws ResultException;
	
}
