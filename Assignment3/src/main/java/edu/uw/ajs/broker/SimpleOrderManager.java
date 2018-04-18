package main.java.edu.uw.ajs.broker;

import java.util.Comparator;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uw.ext.framework.broker.OrderManager;
import edu.uw.ext.framework.broker.OrderQueue;
import edu.uw.ext.framework.order.StopBuyOrder;
import edu.uw.ext.framework.order.StopSellOrder;

public class SimpleOrderManager implements OrderManager {

	/**
	 * Logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(SimpleBroker.class);

	/**
	 * Queue for stop buy orders
	 */
	protected SimpleOrderQueue<Integer, StopBuyOrder> stopBuyOrderQueue;

	/**
	 * Queue for stop sell orders
	 * 
	 */
	protected SimpleOrderQueue<Integer, StopSellOrder> stopSellOrderQueue;

	/**
	 * Stock ticker symbol
	 */
	private String stockTickerSymbol;

	// private int price;

	/**
	 * Constructor. Constructor to be used by sub classes to finish
	 * initialization.
	 * 
	 * @param stockTickerSymbol
	 *            - the ticker symbol of the stock this instance is manage
	 *            orders for
	 */
	public SimpleOrderManager(String stockTickerSymbol) {

		this.stockTickerSymbol = stockTickerSymbol;
	}

	/**
	 * Constructor.
	 * 
	 * @param stockTickerSymbol
	 *            - the ticker symbol of the stock this instance is manage
	 *            orders for
	 * 
	 * @param price
	 *            - the current price of stock to be managed
	 */
	public SimpleOrderManager(String stockTickerSymbol, int price) {
		this.stockTickerSymbol = stockTickerSymbol;

		stopBuyOrderQueue = new SimpleOrderQueue<Integer, StopBuyOrder>(price, (t, o) -> o.getPrice() <= t,
				Comparator.comparing(StopBuyOrder::getPrice).thenComparing(StopBuyOrder::compareTo));

		stopSellOrderQueue = new SimpleOrderQueue<Integer, StopSellOrder>(price, (t, o) -> o.getPrice() >= t,
				Comparator.comparing(StopSellOrder::getPrice).reversed().thenComparing(StopSellOrder::compareTo));

	}

	@Override
	public String getSymbol() {
		return this.stockTickerSymbol;
	}

	@Override
	public void adjustPrice(int price) {
		stopBuyOrderQueue.setThreshold(price);
		stopSellOrderQueue.setThreshold(price);
	}

	@Override
	public void queueOrder(StopBuyOrder order) {
		stopBuyOrderQueue.enqueue(order);
	}

	@Override
	public void queueOrder(StopSellOrder order) {

		stopSellOrderQueue.enqueue(order);
	}

	@Override
	public void setBuyOrderProcessor(Consumer<StopBuyOrder> processor) {

		logger.info("Setting buy order processor as: " + processor.toString());

		stopBuyOrderQueue.setOrderProcessor(processor);
	}

	@Override
	public void setSellOrderProcessor(Consumer<StopSellOrder> processor) {
		stopSellOrderQueue.setOrderProcessor(processor);

	}

}
