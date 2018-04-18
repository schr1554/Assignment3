package main.java.edu.uw.ajs.broker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uw.ext.framework.account.Account;
import edu.uw.ext.framework.account.AccountException;
import edu.uw.ext.framework.account.AccountManager;
import edu.uw.ext.framework.broker.Broker;
import edu.uw.ext.framework.broker.BrokerException;
import edu.uw.ext.framework.broker.OrderManager;
import edu.uw.ext.framework.broker.OrderQueue;
import edu.uw.ext.framework.exchange.ExchangeEvent;
import edu.uw.ext.framework.exchange.ExchangeListener;
import edu.uw.ext.framework.exchange.StockExchange;
import edu.uw.ext.framework.exchange.StockQuote;
import edu.uw.ext.framework.order.MarketBuyOrder;
import edu.uw.ext.framework.order.MarketSellOrder;
import edu.uw.ext.framework.order.Order;
import edu.uw.ext.framework.order.StopBuyOrder;
import edu.uw.ext.framework.order.StopSellOrder;
import main.java.edu.uw.ajs.account.SimpleAccountManager;

public class SimpleBroker implements Broker, ExchangeListener {

	/**
	 * The market order queue.
	 */

	/**
	 * Logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(SimpleBroker.class);

	/**
	 * Name of the broker
	 */
	private String brokerName;

	/**
	 * The Account Manager
	 */
	private AccountManager acctMgr;

	/**
	 * Stock Exchange
	 */
	private StockExchange exchg;

	/**
	 * The name of the broker
	 */
	// private String name;

	protected OrderQueue<Boolean, Order> marketOrders;

	/**
	 * Initial account balance in cents
	 */
	private String balance;

	/**
	 * Account
	 */
	private Account acct;

	/**
	 * List of stocks in exchange
	 */
	private String[] stockList;

	/**
	 * Order Manager Map
	 */
	private HashMap<String, OrderManager> orderManagerMap = new HashMap<String, OrderManager>();

	/**
	 * Constructor.
	 * 
	 * @param brokerName
	 *            - name of the broker
	 * @param acctMngr
	 *            - the account manager to be used by the broker
	 * @param exchg
	 *            - the stock exchange to be used by the broker
	 * 
	 */
	public SimpleBroker(String brokerName, AccountManager acctMgr, StockExchange exchg) {
		this(brokerName, exchg, acctMgr);

		marketOrders = new SimpleOrderQueue<>(exchg.isOpen(), (Boolean t, Order o) -> t);
		Consumer<Order> stockTracker = (order) -> {
			logger.info(String.format("Executing - %s", order));
			int sharePrice = exchg.executeTrade(order);
			try {
				Account account = acctMgr.getAccount(order.getAccountId());
				account.reflectOrder(order, sharePrice);
				logger.info(String.format("New balance - %d", account.getBalance()));
			} catch (AccountException e) {
				logger.error(String.format("Unable to update account %s", order.getAccountId()));
			}
		};
		marketOrders.setOrderProcessor(stockTracker);

		initializeOrderManagers();

		exchg.addExchangeListener(this);
	}

	/**
	 * Constructor. Constructor for sub classes
	 * 
	 * @param brokerName
	 *            - name of the broker
	 * @param acctMgr
	 *            - the stock exchange to be used by the broker
	 * @param exchg
	 *            - the account manager to be used by the broker
	 */
	protected SimpleBroker(String brokerName, StockExchange exchg, AccountManager acctMgr) {
		this.brokerName = brokerName;
		this.acctMgr = acctMgr;
		this.exchg = exchg;

	}

	/**
	 * Fetch the stock list from the exchange and initialize an order manager
	 * for each stock. Only to be used during construction.
	 */
	protected final void initializeOrderManagers() {

		logger.info("Generating order manager map");

		orderManagerMap = new HashMap<>();

		final Consumer<StopBuyOrder> moveBuy2MarketProc = (StopBuyOrder order) -> marketOrders.enqueue(order);

		final Consumer<StopSellOrder> moveSell2MarketProc = (StopSellOrder order) -> marketOrders.enqueue(order);

		for (String ticker : exchg.getTickers()) {

			final int currPrice = exchg.getQuote(ticker).getPrice();

			final OrderManager orderMgr = createOrderManager(ticker, currPrice);

			orderMgr.setBuyOrderProcessor(moveBuy2MarketProc);

			logger.info("Setting buy order processor from list.");

			orderMgr.setSellOrderProcessor(moveSell2MarketProc);

			logger.info("Setting sell order processor from list.");
			orderManagerMap.put(ticker, orderMgr);

		}

	}

	/**
	 * Create an appropriate order manager for this broker. Only to be used
	 * during construction.
	 * 
	 * @param ticker
	 *            - the ticker symbol of the stock
	 * @param initialPrice
	 *            - current price of the stock
	 * @return a new OrderManager for the specified stock
	 * 
	 */
	protected SimpleOrderManager createOrderManager(String ticker, int initialPrice) {

		SimpleOrderManager simpleOrderManager = new SimpleOrderManager(ticker, initialPrice);
		return simpleOrderManager;
	}

	@Override
	public synchronized final void priceChanged(ExchangeEvent event) {
		checkInvariants();

		OrderManager orderMgr;

		orderMgr = orderManagerMap.get(event.getTicker());
		if (orderMgr != null) {
			orderMgr.adjustPrice(event.getPrice());
		}

	}

	@Override
	public void exchangeOpened(ExchangeEvent event) {
		checkInvariants();
		logger.info("Checked Invariants");
		marketOrders.setThreshold(Boolean.TRUE);
	}

	@Override
	public void exchangeClosed(ExchangeEvent event) {
		checkInvariants();
		logger.info("Checked Invariants");
		marketOrders.setThreshold(Boolean.FALSE);
	}

	@Override
	public String getName() {
		return this.brokerName;
	}

	public final Account createAccount(String username, String password, int balance) throws BrokerException {
		checkInvariants();
		try {
			Account newAccount = acctMgr.createAccount(username, password, balance);
			return newAccount;
		} catch (AccountException e) {
			logger.error(String.format("unable to create account for %s", username), e);
			throw new BrokerException(e);
		}
	}

	@Override
	public void deleteAccount(String userName) throws BrokerException {

		try {
			logger.info(String.format("Attempting to delete account for %s", userName));
			acctMgr.deleteAccount(userName);

		} catch (AccountException e) {
			logger.error(String.format("Unable to delete account for %s", userName), e);
			throw new BrokerException(e);

		}
	}

	@Override
	public Account getAccount(String username, String password) throws BrokerException {

		Account account = null;

		try {
			if (acctMgr.validateLogin(username, password)) {

				account = acctMgr.getAccount(username);

				logger.info("Getting account: " + account);
				System.out.println(account);

			} else {
				throw new BrokerException("Invalid Username/password");
			}
		} catch (AccountException e) {
			e.printStackTrace();
		}

		return account;
	}

	@Override
	public StockQuote requestQuote(String ticker) throws BrokerException {

		checkInvariants();
		StockQuote quote = exchg.getQuote(ticker);

		logger.info("Request order: " + ticker);

		if (quote == null) {
			throw new BrokerException(String.format("Quote not available for '%s'.", ticker));
		}
		return quote;

	}

	@Override
	public void placeOrder(MarketBuyOrder order) throws BrokerException {
		checkInvariants();
		logger.info("Place order market buy order: " + order.getAccountId());
		marketOrders.enqueue(order);
	}

	@Override
	public void placeOrder(MarketSellOrder order) throws BrokerException {
		checkInvariants();
		logger.info("Place order market sell order: " + order.getAccountId());
		marketOrders.enqueue(order);
	}

	@Override
	public final void placeOrder(StopBuyOrder order) throws BrokerException {
		checkInvariants();
		logger.info("Place order market buy order: " + order.getAccountId());
		orderManagerLookup(order.getStockTicker()).queueOrder(order);
	}

	@Override
	public final void placeOrder(StopSellOrder order) throws BrokerException {
		checkInvariants();
		logger.info("Place order stop sell order: " + order.getAccountId());
		orderManagerLookup(order.getStockTicker()).queueOrder(order);
	}

	private OrderManager orderManagerLookup(String stockTicker) throws BrokerException {
		OrderManager orderMgr = orderManagerMap.get(stockTicker);
		if (orderMgr == null) {
			throw new BrokerException(String.format("Requested stock, %s does not exist", stockTicker),
					new AccountException());
		}
		return orderMgr;
	}

	@Override
	public void close() throws BrokerException {

		exchg.removeExchangeListener(this);
		try {
			acctMgr.close();
		} catch (AccountException e) {
			e.printStackTrace();
		}
		orderManagerMap = null;

	}

	public boolean checkInvariants() {

		boolean variants = true;

		if (this.acct == null) {
			variants = false;
		}

		if (this.acctMgr == null) {
			variants = false;
		}

		if (this.balance == null) {
			variants = false;
		}

		if (this.brokerName == null) {
			variants = false;
		}

		if (this.exchg == null) {
			variants = false;
		}

		logger.info("Variants set: " + variants);

		return variants;
	}

}
