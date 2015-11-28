package cz.upol.vanusanik.disindent.buildpath;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import cz.upol.vanusanik.disindent.compiler.CompilerUtils;
import cz.upol.vanusanik.disindent.errors.CompilerConstantRedefinitionException;
import cz.upol.vanusanik.disindent.utils.Warner;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentLexer;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.Compiler_argContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.Compiler_argsContext;

public class CompilerOptions implements Serializable {
	private static final long serialVersionUID = 4696697227008294448L;

	private Map<String, Object> internal = new HashMap<String, Object>();
	
	public CompilerOptions(Map<String, Object> initialArgs){
		internal.putAll(initialArgs);
	}
	
	public CompilerOptions(CompilerOptions globalOptions) {
		internal.putAll(globalOptions.internal);
	}

	public static CompilerOptions compileInitialArgs(String line) throws Exception {
		ANTLRInputStream is = new ANTLRInputStream(line == null ? "" : line);
		disindentLexer lexer = new disindentLexer(is);
		CommonTokenStream stream = new CommonTokenStream(lexer);
		disindentParser parser = new disindentParser(stream);
		
		Compiler_argsContext cargs = parser.compiler_args();
		Map<String, Object> initialArgs = new HashMap<String, Object>();
		
		for (Compiler_argContext carg : cargs.compiler_arg()){
			String name = carg.identifier().getText();
			Object value = Boolean.TRUE;
			if (carg.const_arg() != null)
				value = CompilerUtils.asValue(carg.const_arg());
			if (value == null){
				if (initialArgs.containsKey(name))
					initialArgs.remove(name);
				continue;
			}
			initialArgs.put(name, value);
		}
		
		return new CompilerOptions(initialArgs);
	}
	
	public boolean asBoolean(String option){
		return (Boolean)internal.get(option);
	}
	
	public int asInt(String option){
		return (Integer)internal.get(option);
	}
	
	public long asLong(String option){
		return (Long)internal.get(option);
	}
	
	public float asFloat(String option){
		return (Float)internal.get(option);
	}
	
	public double asDouble(String option){
		return (Double)internal.get(option);
	}
	
	public String asString(String option){
		return (String)internal.get(option);
	}
	
	public boolean isBoolean(String option){
		Object value = internal.get(option);
		return value instanceof Boolean;
	}
	
	public boolean isInt(String option){
		Object value = internal.get(option);
		return value instanceof Integer;
	}
	
	public boolean isLong(String option){
		Object value = internal.get(option);
		return value instanceof Long;
	}
	
	public boolean isDouble(String option){
		Object value = internal.get(option);
		return value instanceof Double;
	}
	
	public boolean isFloat(String option){
		Object value = internal.get(option);
		return value instanceof Float;
	}
	
	public boolean isString(String option){
		Object value = internal.get(option);
		return value instanceof String;
	}
	
	public boolean isDefined(String option){
		return internal.containsKey(option);
	}
	
	public void set(String option, Object value, boolean override){
		if (value != null && !override && internal.containsKey(option))
			Warner.warn(new CompilerConstantRedefinitionException("Compiler constant " + option + "redefined"));
		if (value == null)
			internal.remove(option);
		else
			internal.put(option, value);
	}
}
