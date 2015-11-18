package cz.upol.vanusanik.disindent.runtime.network;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.disindent.buildpath.BuildPath;
import cz.upol.vanusanik.disindent.runtime.DisindentException;
import cz.upol.vanusanik.disindent.runtime.types.Method;

public class NetworkExecutor {
	
	/**
	 * Stores socket for current thread here
	 */
	private static ThreadLocal<Socket> socket = new ThreadLocal<Socket>();

	public static void setSocketForThread(Socket s) {
		socket.set(s);
	}
	
	public static Socket getSocketForThread(){
		return socket.get();
	}

	public static NetworkExecutionResult execute(final Method f, int tCount,
			Object[] args) throws Exception {
		
		List<Thread> tList = new ArrayList<Thread>();
		final NetworkExecutionResult result = new NetworkExecutionResult();
		result.results = new Object[tCount];
		result.exceptions = new String[tCount];
		result.exceptionMessages = new String[tCount];

		List<Node> nnodes;
		try {
			nnodes = NodeList.getBestLoadNodes(tCount);
		} catch (Exception e) {
			throw new DisindentException("network_error", "no hosts available");
		}
		final List<Node> nodes = nnodes;
		
		final List<String> transformed = new ArrayList<String>();
    	for (Object o : args){
    		transformed.add(NetworkUtils.serialize(o));
    	}
    	final String bp = NetworkUtils.serialize(BuildPath.getBuildPath());
    	final String bpUid = BuildPath.getBuildPath().getUid();
		
    	for (int i = 0; i < tCount; i++) {
			final int tId = i;
			tList.add(new Thread(new Runnable() {

				@Override
				public void run() {
					handleDistributedCall(
							tId,
							result,
							getOrFail(nodes, tId),
							transformed,
							f, bp, bpUid,
							tCount);
				}

				private Node getOrFail(List<Node> nodes, int tId) {
					if (tId < nodes.size())
						return nodes.get(tId);
					return null;
				}

			}));
		}

		for (Thread t : tList)
			t.start();

		for (Thread t : tList)
			try {
				t.join();
			} catch (InterruptedException e) {

			}

		return result;
	}

	private static void handleDistributedCall(int tId,
			NetworkExecutionResult result, Node node,
			List<String> arguments, Method f, String bp, String bpUid, int tCount) {
		
		boolean executed = false;
		int rqc = 0;
		Socket s = null;

		outer:
		do {
			try {
				if (node == null){ // no node available
					result.exceptions[tId] = "network_error";
					result.exceptionMessages[tId] = "no hosts available";
					result.results[tId] = null;
					return;
				}
				
				if (NodeList.isUseSSL())
					s = SSLSocketFactory.getDefault().createSocket(
							node.getAddress(), node.getPort());
				else
					s = SocketFactory.getDefault().createSocket(
							node.getAddress(), node.getPort());
				Protocol.send(s.getOutputStream(), new JsonObject().add(
						"header", Protocol.RESERVE_SPOT_REQUEST)
						.add("payload", new JsonObject().add("bpUid", bpUid)));
				JsonObject response = Protocol.receive(s.getInputStream());

				if (!response.getString("header", "").equals(
						Protocol.RESERVE_SPOT_RESPONSE))
					continue; // bad chain reply

				if (!response.get("payload").asObject()
						.getBoolean("result", false)) {
					if (rqc++ > 10){
						// no nodes available at all because they always get taken, throw exception back to the system
						result.exceptions[tId] = "network_error";
						result.exceptionMessages[tId] = "no hosts available";
						result.results[tId] = null;
						return;
					}
					node = NodeList.getRandomNode();
					continue; // no reserved thread for me :(
				}
				
				boolean needsBP = response.get("payload").asObject()
						.getBoolean("requestingBuildPath", false);

				JsonValue args = new JsonArray();
				for (String arg : arguments)
					args.asArray().add(arg);
				JsonObject payload = new JsonObject().add("header",
						Protocol.RUN_CODE).add(
						"payload",
						new JsonObject()
								.add("uid", bpUid)
								.add("buildPath", needsBP ? bp : "")
								.add("runnerClass", f.clazz.getName()).add("args", args)
								.add("id", tId).add("methodName", f.methodName));
				Protocol.send(s.getOutputStream(), payload);

				while (true){
					response = Protocol.receive(s.getInputStream());
					
					if (response == null)
						continue outer;
	
					if (response.getString("header", "").equals(
							Protocol.RETURNED_EXECUTION)){
						payload = response.get("payload").asObject();
						break;
					}					
					continue outer; // bad chain reply
				}

				if (payload.getBoolean("hasResult", false)) {
					result.results[tId] = NetworkUtils.deserialize(payload.getString("result", ""), Object.class);
					result.exceptions[tId] = null;
				} else {
					result.exceptions[tId] = payload.getString("exceptionType", "");
					result.exceptionMessages[tId] = payload.getString("exceptionMessage", "");
					result.results[tId] = null;
				}

				executed = true;
			} catch (Exception e) {
				node = NodeList.getRandomNode(); // Refresh node since error
													// might have been node
													// related
				executed = false;
			} finally {
				if (s != null) {
					try {
						s.close();
					} catch (Exception e) {
						// ignore
						e.printStackTrace();
					}
				}
			}
		} while (!executed);
		
	}

}
