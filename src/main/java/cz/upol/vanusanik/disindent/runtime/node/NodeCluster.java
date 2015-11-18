package cz.upol.vanusanik.disindent.runtime.node;

/**
 * NodeCluster class contains all the nodes for this Node Controller. You can
 * get free nodes via getFreeNode() method, which will also reserve such a node.
 * 
 * @author Peter Vanusanik
 *
 */
public class NodeCluster {
	
	public static class ___TimeoutException extends RuntimeException {
		private static final long serialVersionUID = 7423384920348882357L;
		private int timeoutValue;
		public ___TimeoutException(int timeout){
			this.timeoutValue = timeout;
		}
		public int getTimeoutValue(){
			return timeoutValue;
		}
	}

	private Node[] nodes;
	private Thread[] tcontainers;
	private int timeout;

	public NodeCluster(final int wtc, final int timeout) {
		this.timeout = timeout;
		nodes = new Node[wtc];
		tcontainers = new Thread[wtc];
		for (int i = 0; i < wtc; i++) {
			newNode(i);
		}
		if (timeout > 0){
			Thread cleaner = new Thread("Cleaning daemon thread"){

				@Override
				public void run() {
					while (!interrupted()){
						for (int i=0; i<wtc; i++){
							Node n = nodes[i];
							if (n.isExceedingTimeout(timeout))
								terminateNode(n);
						}
					}
				}
				
			};
			cleaner.setDaemon(true);
			cleaner.setPriority(Thread.MIN_PRIORITY);
			cleaner.start();
		}
	}

	/**
	 * Creates new node at slot i
	 * @param i
	 */
	private void newNode(int i) {
		nodes[i] = new Node(this, i);
		Thread t = new Thread(nodes[i], "Worker Thread " + i + ";");
		tcontainers[i] = t;
		t.setDaemon(true);
		t.start();
	}

	/**
	 * Returns all nodes.
	 * 
	 * @return all nodes
	 */
	public Node[] getNodes() {
		return nodes;
	}
	
	/**
	 * Terminates node n, used by daemon
	 * @param n
	 */
	public synchronized void terminateNode(Node n){
		for (int i=0; i<nodes.length; i++){
			if (nodes[i] == n){
				terminateNode(i);
				break;
			}
		}
	}

	/**
	 * Terminates single node i
	 * @param i
	 */
	@SuppressWarnings("deprecation")
	private void terminateNode(int i) {
		tcontainers[i].stop(new ___TimeoutException(timeout));
		newNode(i);
	}

	int rrindex = 0;

	/**
	 * Returns free node via round robin queue mechanism. May return null if no
	 * free nodes exists. After 4 * maxAmountOfNodes it will switch to linear
	 * queue algorithm for finding free node.
	 * 
	 * @return free, reserved node or null
	 */
	public synchronized Node getFreeNode() {
		Node n;
		int it = 0;
		do {
			if (it > 4 * nodes.length)
				return getFreeNodeOrFail();
			n = nodes[rrindex];
			rrindex = (rrindex + 1) % nodes.length;
			++it;
		} while (n.isBusy());
		n.reserve();
		return n;
	}

	/**
	 * Linearly finds a first free node or fails and returns null
	 * 
	 * @return free, reserved node or null
	 */
	private synchronized Node getFreeNodeOrFail() {
		for (Node n : nodes) {
			if (!n.isBusy()) {
				n.reserve();
				return n;
			}
		}
		return null;
	}
}
