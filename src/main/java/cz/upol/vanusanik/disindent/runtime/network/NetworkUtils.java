package cz.upol.vanusanik.disindent.runtime.network;

import java.lang.reflect.Modifier;
import java.net.Socket;

import com.eclipsesource.json.JsonObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class NetworkUtils {

	/**
	 * Sends the error back to the client.
	 * 
	 * This is not used to send back exception, instead it is used to send when
	 * error happened prior to the running of the runtime.
	 * 
	 * @param s
	 * @param payload
	 * @param ecode
	 * @param dmesg
	 * @throws Exception
	 */
	public static void sendError(Socket s, JsonObject payload, long ecode,
			String dmesg) throws Exception {
		payload.add("payload",
				new JsonObject().add("error", true).add("errorCode", ecode)
						.add("errorDetails", dmesg));
		Protocol.send(s.getOutputStream(), payload);
	}
	
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
	
	private static Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).create();

	public static String serialize(Object result) {
		return gson.toJson(result);
	}

	public static <T> T deserialize(String json, Class<? extends T> clazz){
		return gson.fromJson(json, clazz);
	}
}
