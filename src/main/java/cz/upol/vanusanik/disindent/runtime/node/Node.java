package cz.upol.vanusanik.disindent.runtime.node;

import org.apache.log4j.Logger;

import cz.upol.vanusanik.disindent.runtime.node.NodeCluster.___TimeoutException;

/**
 * Node is running thread that handles the execution of Disindent code.
 * 
 * @author Peter Vanusanik
 *
 */
public class Node implements Runnable {
	private static final Logger log = Logger.getLogger(Node.class);

	/** Id of this Node */
	private int id;
	/** Whether this node is reserved for some client or not */
	private boolean reserved = false;
	/** Start time of the payload, used to terminate halting threads */
	private long startTime;

	public Node(NodeCluster nodeCluster, int id) {
		this.setId(id);
	}

	public synchronized boolean isBusy() {
		return payload != null || reserved;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	/** Payload that will be run asap */
	private volatile Runnable payload = null;

	@Override
	public void run() {
		while (true) {
			try {
				while (payload == null)
					Thread.sleep(10);
				payload.run();
				reserved = false;
				payload = null;
			} catch (___TimeoutException e){
				payload = null;
				reserved = false;
				return;
			} catch (Exception e) {
				payload = null;
				reserved = false;
				log.error(e, e);
			}
		}
	}

	/**
	 * Sets the new payload. This method does not check whether there is payload
	 * already running and setting new payload while payload is running results
	 * in indefinite behavior.
	 * 
	 * @param payload
	 *            to be run
	 */
	public synchronized void setNewPayload(Runnable payload) {
		startTime = System.currentTimeMillis();
		this.payload = payload;
	}

	/**
	 * Reserves this node
	 */
	public synchronized void reserve() {
		reserved = true;
	}

	/**
	 * Releases this node, if it was previously reserved. Does not unblocks the
	 * thread or node if it is still computing!
	 */
	public void release() {
		reserved = false;
	}

	/**
	 * Returns true if thread is exceeding timeout
	 * @param timeout
	 * @return
	 */
	public synchronized boolean isExceedingTimeout(int timeout) {
		return (startTime + timeout < System.currentTimeMillis()) && (payload != null); 
	}
}
