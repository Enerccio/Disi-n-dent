package cz.upol.vanusanik.disindent.runtime.node;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.apache.log4j.Logger;

import com.beust.jcommander.JCommander;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import cz.upol.vanusanik.disindent.buildpath.BuildPath;
import cz.upol.vanusanik.disindent.runtime.DisindentException;
import cz.upol.vanusanik.disindent.runtime.network.NetworkExecutor;
import cz.upol.vanusanik.disindent.runtime.network.NetworkUtils;
import cz.upol.vanusanik.disindent.runtime.network.NodeList;
import cz.upol.vanusanik.disindent.runtime.network.Protocol;
import cz.upol.vanusanik.disindent.runtime.node.NodeCluster.___TimeoutException;
import cz.upol.vanusanik.disindent.runtime.types.Method;

/**
 * NodeController is main class of the Disindent Node.
 * 
 * NodeController controls the runtime of Disindent Node, providing services to
 * whomever connects to it. The communication is handled by extended json
 * protocol, which is 0x1e <4 bytes> <json message>. NodeController can be
 * modified via settings in NodeOptions.
 * 
 * @author Peter Vanusanik
 *
 */
public class NodeController {
	private static final Logger log = Logger.getLogger(NodeController.class);

	public static final void main(String[] args) throws Exception {
		NodeOptions no = new NodeOptions();
		new JCommander(no, args);

		NodeController nc = new NodeController();
		try {
			nc.start(no);
		} finally {
			nc.service.shutdown();
		}
	}

	/**
	 * service is thread pool that handles incoming requests.
	 */
	private ExecutorService service;
	/**
	 * cluster contains disindent nodes. You can retrieve free nodes from it.
	 */
	private NodeCluster cluster;
	
	private Map<String, SoftReference<BuildPath>> buildPathCache = Collections.synchronizedMap(new HashMap<String, SoftReference<BuildPath>>());

	/**
	 * Starts this node controller instance server, will start to accept the
	 * incoming requests.
	 * 
	 * @param no
	 * @throws Exception
	 */
	private void start(NodeOptions no) throws Exception {
		log.info("Starting Disindent Node Controller at port " + no.portNumber
				+ ", number of working threads: " + no.threadCount);

		initialize(no);

		ServerSocket server = createServer(no);

		while (true) {
			final Socket s = server.accept();
			s.setKeepAlive(true);
			service.execute(new Runnable() {

				@Override
				public void run() {
					try {
						localStorage.set(new NodeLocalStorage());
						while (!s.isClosed())
							if (resolveRequest(s))
								break;
					} catch (Exception e) {
						log.error(e);
						log.debug(e, e);
					} finally {
						NodeLocalStorage local = localStorage.get();
						localStorage.set(null);
						if (local != null && local.reservedNode != null)
							local.reservedNode.release();
						try {
							s.close();
						} catch (IOException e) {
							log.error(e);
						}
					}
				}

			});
		}
	}

	private ServerSocket createServer(NodeOptions no) throws Exception {
		ServerSocket server;
		if (no.useSSL){
			SSLServerSocketFactory sslServerFactory = (SSLServerSocketFactory) SSLServerSocketFactory
					.getDefault();
			server = sslServerFactory
					.createServerSocket(no.portNumber, 50);
			((SSLServerSocket)server).getNeedClientAuth();
			return server;
		} else 
			server = ServerSocketFactory.getDefault().createServerSocket(no.portNumber);
		return server;
	}

	/**
	 * Request Data storage class. Every client that has open request stream
	 * will have one assigned. Used to store intermediate data for client on
	 * this server.
	 * 
	 * @author Peter Vanusanik
	 *
	 */
	private class NodeLocalStorage {
		/** Node that this client has reserved, if any */
		public Node reservedNode;
		public BuildPath bp;
		public DisindentException exception;
	}

	/** NodeLocalStorage is stored in this thread local */
	private ThreadLocal<NodeLocalStorage> localStorage = new ThreadLocal<NodeLocalStorage>();

	/**
	 * Resolves request from client. Will not close connection (only client can
	 * do that or timeout).
	 * 
	 * @param s
	 *            communication bound socket
	 * @return whether the communication was closed or not
	 * @throws Exception
	 *             if communication fails
	 */
	protected boolean resolveRequest(Socket s) throws Exception {
		s.setSoTimeout(1000 * 120);
		JsonObject m = Protocol.receive(s.getInputStream());

		if (m == null)
			return true;

		log.info("Request " + m.get("header") + " from " + s.getLocalAddress());

		if (m.getString("header", "").equals(Protocol.GET_STATUS_REQUEST))
			resolveStatusRequest(s, m);
		if (m.getString("header", "").equals(Protocol.RESERVE_SPOT_REQUEST))
			resolveReserveSpotRequest(s, m);
		if (m.getString("header", "").equals(Protocol.RUN_CODE))
			resolveRunCode(s, m);
		return false;
	}

	private void resolveRunCode(final Socket s, JsonObject m) throws Exception {
		JsonObject payload = new JsonObject();
		payload.add("header", Protocol.RETURNED_EXECUTION);

		final NodeLocalStorage storage = localStorage.get();
		if (storage.reservedNode == null) {
			NetworkUtils.sendError(s, payload, Protocol.ERROR_NO_RESERVED_NODE,
					"No reserved node for this client");
			return;
		}

		final JsonObject input = m.get("payload").asObject();
		if (storage.bp == null){
			storage.bp = NetworkUtils.deserialize(input.getString("buildPath", null), BuildPath.class);
			String uid = input.getString("uid", null);
			buildPathCache.put(uid, new SoftReference<BuildPath>(storage.bp));
		}

		/* Initialize the runtime */
		// TODO
		storage.exception = null;
		
		final List<String> args = new ArrayList<String>();
		JsonArray a = input.get("args").asArray();
		for (int i=0; i<a.size(); i++)
			args.add(a.get(i).asString());

		/* Runtime is prepared to resume from last call */
		RunnablePayload<Object> run = new RunnablePayload<Object>() {

			@Override
			protected Object executePayload() {
				BuildPath.setForThread(storage.bp);
				NetworkExecutor.setSocketForThread(s);
				
				try {
					Method m = NetworkUtils.deserialize(input.getString("method", null), Method.class);
					Object[] a = new Object[args.size() + 1];
					int it = 0;
					
					a[it++] = input.getInt("id", 0);
					for (String arg : args)
						a[it++] = NetworkUtils.deserialize(arg, Object.class);
					return m.invoke(a);
				} catch (DisindentException e) {
					e.printStackTrace();
					storage.exception = e;
					return null;
				} catch (___TimeoutException te){
					// cancel the thread due to time limit and inform about it
					log.info("Worker thread failed due to timeout");
					storage.exception = new DisindentException("network_timeout", "Timeout, maximum runtime allowed: " + te.getTimeoutValue());
					finished = true;
					throw te;
				} catch (Exception e){
					e.printStackTrace();
					storage.exception = new DisindentException("error", e.getMessage());
					finished = true;
					return null;
				} catch (Throwable t) {
					// cancel unlimited running because throwing will go behind
					// the usual end of run mechanics
					finished = true;
					return null;
				}
			}

		};

		// Runs the code
		storage.reservedNode.setNewPayload(run);

		// Wait for the payload to finish with active waiting
		while (!run.hasFinished()) {
			Thread.sleep(10);
		}

		log.info("Finished running");
		Object result = run.getResult();

		// serialize the return value back to the caller
		JsonObject p = new JsonObject();
		if (result != null) {
			p.add("hasResult", true);
			p.add("result", NetworkUtils.serialize(result));
		} else {
			p.add("hasResult", false);
			p.add("exceptionType", storage.exception.getIndentifier());
			p.add("exceptionMessage", storage.exception.getMessage());
		}

		payload.add("payload", p);
		localStorage.set(null);

		// send the payload back to the requesting client
		Protocol.send(s.getOutputStream(), payload);
	}

	/**
	 * Resolve reserve spot request. Will reserve a node for this client or
	 * fails, and reports back the success or failure via
	 * Protocol.RESERVE_SPOT_RESPONSE message.
	 * 
	 * @param s
	 *            communication bound socket
	 * @param m
	 *            original message
	 * @throws Exception
	 *             on any failure
	 */
	private void resolveReserveSpotRequest(Socket s, JsonObject m)
			throws Exception {
		JsonObject payload = new JsonObject();
		payload.add("header", Protocol.RESERVE_SPOT_RESPONSE);

		NodeLocalStorage storage = localStorage.get();

		storage.reservedNode = cluster.getFreeNode();
		String uid = m.get("payload").asObject().getString("bpUid", null);
		
		synchronized (buildPathCache){
			if (buildPathCache.containsKey(uid)){
				storage.bp = buildPathCache.get(uid).get();
				if (storage.bp == null && buildPathCache.containsKey(uid))
					buildPathCache.remove(uid);
			}
		}
		
		if (storage.reservedNode == null) {
			payload.add("payload", new JsonObject().add("result", false));
		} else {
			payload.add("payload", new JsonObject().add("result", true).add("requestingBuildPath", storage.bp == null));
		}
		Protocol.send(s.getOutputStream(), payload);
	}

	/**
	 * Sends information about this node to the client via
	 * Protocol.GET_STATUS_RESPONSE message.
	 * 
	 * @param s
	 *            communication bound socket
	 * @param m
	 *            original message
	 * @throws Exception
	 *             on any failure
	 */
	private void resolveStatusRequest(Socket s, JsonObject m) throws Exception {
		JsonObject payload = new JsonObject().add("header",
				Protocol.GET_STATUS_RESPONSE).add(
				"payload",
				new JsonObject().add("workerThreads", options.threadCount).add(
						"workerThreadStatus", generateWorkerThreadUsage()));
		Protocol.send(s.getOutputStream(), payload);
	}

	/** Helper method to compute worker thread usage message */
	private JsonObject generateWorkerThreadUsage() {
		JsonObject o = new JsonObject();

		for (Node n : cluster.getNodes()) {
			synchronized (cluster){
				o.add("node" + n.getId(), n.isBusy());
			}
		}

		return o;
	}

	private NodeOptions options;

	/**
	 * Initializes this NodeController via NodeOptions. Also creates the
	 * handling thread pools and RuntimeMemoryCleaningThread cleaning thread
	 * that is immediately started.
	 * 
	 * @param no
	 *            initialization values.
	 * @throws Exception
	 */
	private void initialize(NodeOptions no) throws Exception {
		service = Executors.newCachedThreadPool();
		options = no;
		cluster = new NodeCluster(no.threadCount, no.timeout);

		if (no.nodeListFile != null) {
			FileInputStream fis = new FileInputStream(no.nodeListFile);
			NodeList.loadFile(fis);
			fis.close();
		}

		String[] parsedNodes = no.nodes.split(";");
		for (String s : parsedNodes) {
			if (!s.equals("")) {
				String[] datum = s.split(":");
				NodeList.addNode(datum[0], Integer.parseInt(datum[1]));
			}
		}

		if (no.useSSL){
			// Set up the keystores for ssl/tsl communication
			System.setProperty("javax.net.ssl.keyStore", no.keystore);
			System.setProperty("javax.net.ssl.keyStorePassword", no.keystorepass);
		}
		
		NodeList.setUseSSL(no.useSSL);
		
		NodeList.addNode("localhost", no.portNumber);
	}

}
