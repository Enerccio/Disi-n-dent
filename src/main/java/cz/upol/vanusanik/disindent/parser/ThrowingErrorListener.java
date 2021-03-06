package cz.upol.vanusanik.disindent.parser;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 * Throws the exception within antrl instead of continuing with parsing
 * @author Peter Vanusanik
 *
 */
public class ThrowingErrorListener extends BaseErrorListener {
	private String source;

	public ThrowingErrorListener(String loc) {
		this.source = loc;
	}

	@Override
	public void syntaxError(Recognizer<?, ?> recognizer,
			Object offendingSymbol, int line, int charPositionInLine,
			String msg, RecognitionException e)
			throws ParseCancellationException {
		throw new ParseCancellationException("file " + source + " line " + line
				+ ":" + charPositionInLine + " " + msg);
	}
}