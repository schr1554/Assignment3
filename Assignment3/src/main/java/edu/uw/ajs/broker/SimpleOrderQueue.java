package main.java.edu.uw.ajs.broker;

import java.util.Comparator;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uw.ext.framework.broker.OrderQueue;
import edu.uw.ext.framework.order.Order;

/**
 * A simple OrderQueue implementation backed by a TreeSet.
 * 
 * @author chq-alexs
 *
 * @param <T>
 *            T - the dispatch threshold type
 * 
 * @param <E>
 *            E - the type of order contained in the queue
 */
public final class SimpleOrderQueue<T, E extends Order> extends Object implements OrderQueue<T, E> {

	/**
	 * Logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(SimpleBroker.class);

	/**
	 * Queue
	 */
	private TreeSet<E> queue;

	/**
	 * Filter
	 */
	private BiPredicate<T, E> filter;

	/**
	 * Order Processor
	 */
	private Consumer<E> orderProcessor;

	/**
	 * Threshold
	 */
	private T threshold;

	/**
	 * Constructor.
	 * 
	 * @param threshold
	 *            - the initial threshold
	 * @param filter
	 *            - the dispatch filter used to control dispatching from this
	 *            queue
	 */
	public SimpleOrderQueue(T threshold, BiPredicate<T, E> filter) {
		queue = new TreeSet<E>();
		this.threshold = threshold;
		this.filter = filter;
	}

	/**
	 * Constructor.
	 * 
	 * @param threshold
	 *            - the initial threshold
	 * 
	 * @param filter
	 *            - the dispatch filter used to control dispatching from this
	 *            queue
	 * 
	 * @param cmp
	 *            - Comparator to be used for ordering
	 * 
	 */
	public SimpleOrderQueue(T threshold, java.util.function.BiPredicate<T, E> filter, Comparator<E> cmp) {
		queue = new TreeSet<>(cmp);
		this.threshold = threshold;
		this.filter = filter;
	}

	@Override
	public void enqueue(E order) {

		queue.add(order);
		dispatchOrders();

	}

	@Override
	public E dequeue() {

		E order = null;

		if (!queue.isEmpty()) {
			order = queue.first();

			if (filter.test(threshold, order)) {
				queue.remove(queue.first());
				// queue.remove(order);

			} else {
				order = null;
			}
		}

		return order;
	}

	@Override
	public void dispatchOrders() {

		logger.info("Dispatching orders");

		E order;

		while ((order = dequeue()) != null) {
			if (orderProcessor != null) {
				orderProcessor.accept(order);
			}
		}

	}

	@Override
	public void setOrderProcessor(Consumer<E> proc) {
		this.orderProcessor = proc;
		logger.info("Setting buy order processor as: " + proc.toString());

	}

	@Override
	public final void setThreshold(T threshold) {
		this.threshold = threshold;
		dispatchOrders();

	}

	@Override
	public final T getThreshold() {
		return this.threshold;
	}

}
