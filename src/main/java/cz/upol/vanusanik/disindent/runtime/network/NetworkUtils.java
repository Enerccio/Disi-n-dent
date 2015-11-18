package cz.upol.vanusanik.disindent.runtime.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.net.Socket;

import org.apache.commons.codec.binary.Base64;

import com.eclipsesource.json.JsonObject;

import cz.upol.vanusanik.disindent.buildpath.BuildPath;

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
	

	public static String serialize(Object result) throws Exception {
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		ObjectOutputStream oo = new ObjectOutputStream(bs);
		oo.writeObject(result);
		return new String(Base64.encodeBase64(bs.toByteArray()), "utf-8");
	}

	@SuppressWarnings("unchecked")
	public static <T> T deserialize(String json, Class<? extends T> clazz) throws Exception{
		ByteArrayInputStream is = new ByteArrayInputStream(Base64.decodeBase64(json.getBytes("utf-8")));
		ObjectInputStream oo = new ObjectInputStream(is){

			@Override
			protected Class<?> resolveClass(ObjectStreamClass desc)
					throws IOException, ClassNotFoundException {
				if (BuildPath.isEmpty())
					return Class.forName(desc.getName(), false, Thread.currentThread().getContextClassLoader());
				return 
					Class.forName(desc.getName(), false, BuildPath.getBuildPath().getClassLoader());
			}
			
		};
		return (T) oo.readObject();
	}
}
