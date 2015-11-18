package cz.upol.vanusanik.disindent.runtime.node;

/**
 * Base runnable payload class. Will give access to returned value and whether
 * runnable has finished or not
 * 
 * @author Peter Vanusanik
 *
 * @param <T>
 *            value that executePayload should return
 */
public abstract class RunnablePayload<T> implements Runnable {

	protected volatile boolean finished = false;
	private T result;

	/**
	 * @return whether this RunnablePayload has finished executing or not
	 */
	public boolean hasFinished() {
		return finished;
	}

	/**
	 * @return null or result of running this RunnablePayload
	 */
	public T getResult() {
		return result;
	}

	@Override
	public void run() {
		result = executePayload();
		finished = true;
	}

	/**
	 * executes this RunnablePayload
	 * 
	 * @return result of this execution
	 */
	protected abstract T executePayload();

}
