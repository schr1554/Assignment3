package main.java.edu.uw.ajs.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uw.ext.framework.account.AccountManager;
import edu.uw.ext.framework.broker.Broker;
import edu.uw.ext.framework.broker.BrokerFactory;
import edu.uw.ext.framework.exchange.StockExchange;
import main.java.edu.uw.ajs.account.SimpleAccount;

public class SimpleBrokerFactory implements BrokerFactory {

	/** The logger to be used by this class */
	private static final Logger logger = LoggerFactory.getLogger(SimpleAccount.class);

	@Override
	public Broker newBroker(String name, AccountManager acctMngr, StockExchange exch) {

		logger.info("Name: " + name + " Account Manager: " + " Stock Exchange: " + exch);

		return new SimpleBroker(name, acctMngr, exch);

	}

}
