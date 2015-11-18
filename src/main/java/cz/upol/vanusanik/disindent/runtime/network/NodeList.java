package cz.upol.vanusanik.disindent.runtime.network;

import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.io.IOUtils;

import com.eclipsesource.json.JsonObject;

/**
 * Class that manages the nodes. Static class only, cannot be instanced
 * 
 * @author Peter Vanusanik
 *
 */
public final class NodeList {

	private NodeList() {

	}
	
	/**	Whether or not to use SSL */
	private static boolean useSSL = false;

	/** Nodes stored as set */
	private static Set<Node> nodes = new HashSet<Node>();
	/** Nodes stored as list */
	private static List<Node> nList = new ArrayList<Node>();

	/**
	 * Loads nodes from input stream as line delimited address:port
	 * 
	 * @param file
	 * @throws Exception
	 */
	public static void loadFile(InputStream file) throws Exception {
		List<String> lines = IOUtils.readLines(file);
		for (String line : lines) {
			String[] ldata = line.split(":");
			Node n = new Node(ldata[0], Integer.parseInt(ldata[1]));
			nodes.add(n);
			nList.add(n);
		}
	}

	/**
	 * Adds single node
	 * 
	 * @param address
	 * @param port
	 */
	public static void addNode(String address, int port) {
		Node n = new Node(address, port);
		nodes.add(n);
		nList.add(n);
	}

	/**
	 * NodeComparator based on the how many free nodes a node has.
	 * 
	 * @author Peter Vanusanik
	 *
	 */
	private static class NodeComparator implements Comparable<NodeComparator> {
		public Node n;
		public int val;

		@Override
		public int compareTo(NodeComparator arg0) {
			return Integer.compare(val, arg0.val);
		}

	}

	/**
	 * Returns list of reserved nodes
	 * 
	 * @param reqNodeNum
	 *            number of reserved nodes requested
	 * @return
	 */
	public static synchronized List<Node> getBestLoadNodes(int reqNodeNum) {
		List<NodeComparator> ncl = new ArrayList<NodeComparator>();
		for (Node n : nodes) {
			NodeComparator nc = new NodeComparator();
			nc.n = n;
			try {
				nc.val = getFreeNodes(n);
			} catch (Exception e) {
				continue;
			}
			ncl.add(nc);
		}

		List<Node> nodeList = new ArrayList<Node>(reqNodeNum);

		try {
			for (int i = 0; i < reqNodeNum; i++) {
				Collections.sort(ncl);

				nodeList.add(ncl.get(ncl.size() - 1).n);
				--ncl.get(ncl.size() - 1).val;
			}
		} catch (Exception e) {

		}
		return nodeList;
	}

	private static final Random r = new Random();

	/**
	 * Returns random reserved node
	 * 
	 * @return
	 */
	public static synchronized Node getRandomNode() {
		boolean hasFreeNodes = false;
		Node n;
		long ttime = System.nanoTime();
		long ctime = System.nanoTime();
		do {
			try {
				Thread.sleep(0, 10);
			} catch (InterruptedException e1) {

			}
			n = nList.get(r.nextInt(nList.size()));
			try {
				hasFreeNodes = getFreeNodes(n) > 0;
			} catch (Exception e) {
				e.printStackTrace();
			}
			ctime = System.nanoTime();
			if (ctime - ttime > 2000000000) // 2s
				return null;
		} while (hasFreeNodes == false);
		return n;
	}

	/**
	 * Returns number of free worker threads for a node
	 * 
	 * @param n
	 * @return
	 * @throws Exception
	 */
	private static synchronized  int getFreeNodes(Node n) throws Exception {
		Socket s = null;
		try {
			int cache = n.checkLastModified(System.nanoTime());
			if (cache != -1)
				return cache;
			
			// new Exception("req. amount " + n + " : " + Thread.currentThread().getId()).printStackTrace();
			
			if (useSSL)
				s = SSLSocketFactory.getDefault().createSocket(n.getAddress(),
					n.getPort());
			else
				s = SocketFactory.getDefault().createSocket(n.getAddress(),
						n.getPort());
	
			JsonObject o = new JsonObject();
			o.add("header", Protocol.GET_STATUS_REQUEST);
			Protocol.send(s.getOutputStream(), o);
	
			o = Protocol.receive(s.getInputStream());
			if (!o.getString("header", "").equals(Protocol.GET_STATUS_RESPONSE))
				throw new Exception("Wrong reply from the server");
	
			int wtc = o.get("payload").asObject().get("workerThreads").asInt();
			int c = 0;
	
			for (int i = 0; i < wtc; i++) {
				if (!o.get("payload").asObject().get("workerThreadStatus")
						.asObject().getBoolean("node" + i, false))
					++c;
			}
			
			n.setLastModified(System.nanoTime(), c);
			
			return c;
		} finally {
			if (s != null)
				s.close();
		}
	}

	/**
	 * Returns estimated number of workers available
	 * 
	 * @return
	 */
	public static synchronized int expectNumberOfNodes() {
		int sum = 0;
		for (Node n : nList)
			try {
				sum += getFreeNodes(n);
			} catch (Exception e) {

			}
		return sum;
	}

	public static boolean isUseSSL() {
		return useSSL;
	}

	public static void setUseSSL(boolean useSSL) {
		NodeList.useSSL = useSSL;
	}
	
}
