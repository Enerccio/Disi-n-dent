import java.io.Serializable;

import cz.upol.vanusanik.disindent.runtime.DisindentClassLoader;
import cz.upol.vanusanik.disindent.runtime.network.NetworkUtils;


public class Test {
	
	@SuppressWarnings("serial")
	static class XXX implements Serializable { byte[] vvv = new byte[]{0, 1, 2, 3}; };

	public static void main(String[] args) throws Exception {
		NetworkUtils.deserialize(NetworkUtils.serialize(new XXX()), XXX.class);
	}

}
