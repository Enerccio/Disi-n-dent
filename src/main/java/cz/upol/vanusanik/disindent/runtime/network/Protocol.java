package cz.upol.vanusanik.disindent.runtime.network;

import java.io.EOFException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;

/**
 * Static class only. Protocol handles communication between client and node.
 * 
 * @author Peter Vanusanik
 *
 */
public class Protocol {

	// Headers
	public static final String GET_STATUS_REQUEST = "StatusRequest";
	public static final String GET_STATUS_RESPONSE = "StatusResponse";
	public static final String RESERVE_SPOT_REQUEST = "ReserveSpotRequest";
	public static final String RESERVE_SPOT_RESPONSE = "ReserveSpotResponse";
	public static final String RUN_CODE = "RunCode";
	public static final String RETURNED_EXECUTION = "ReturnedExecution";
	public static final String REQUEST_DATA = "RequestData";
	public static final String SEND_DATA = "SendData";

	// Error codes
	public static final long ERROR_GENERAL_ERROR = 0x0;
	public static final long ERROR_NO_RESERVED_NODE = 0x1;
	public static final long ERROR_COMPILATION_FAILURE = 0x2;
	public static final long ERROR_DESERIALIZATION_FAILURE = 0x3;
	public static final long ERROR_UNKNOWN_OBJECT = 0x4;

	/**
	 * Receives payload from socket stream
	 * 
	 * @param inputStream
	 *            input socket stream
	 * @return JsonObject payload
	 * @throws Exception
	 */
	public static JsonObject receive(final InputStream inputStream)
			throws Exception {
		
		synchronized (inputStream){
			
			byte[] lheader = new byte[20];
			try {
				IOUtils.readFully(inputStream, lheader);
			} catch (EOFException e){
				return null;
			}
	
			byte[] data = null;
			try {
				data = new byte[Integer.parseInt(new String(lheader, "utf-8").trim())];
			} catch (NumberFormatException e){
				return null;
			}
			IOUtils.readFully(inputStream, data);
			
			String s = new String(data, "utf-8");
			JsonObject result = Json.parse(s).asObject();
			
			return result;
		}
	}

	/**
	 * Sends the payload to the socket stream
	 * 
	 * @param os
	 *            Socket stream
	 * @param payload
	 *            JsonObject payload
	 * @throws Exception
	 */
	public static void send(OutputStream os, JsonObject payload)
			throws Exception {
		
		synchronized (os){

			String data = payload.toString(WriterConfig.PRETTY_PRINT);
			byte[] ll = data.getBytes("utf-8");
			Integer length = ll.length;
			String content = String.format("%d", length);
			
			byte[] lheader = new byte[20 + length];
			byte[] lsource = content.getBytes("utf-8");
			for (int i=0; i<20; i++){
				if (i < lsource.length)
					lheader[i] = lsource[i];
				else
					lheader[i] = ' ';
			}
			
			for (int i=0; i<length; i++)
				lheader[i+20] = ll[i];
			
			IOUtils.write(lheader, os);
			os.flush();
		}
	}

}
