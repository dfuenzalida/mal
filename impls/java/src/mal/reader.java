package mal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mal.types.MalList;
import mal.types.MalType;

public class reader {
	
	static String TOKENS_REGEX = "[\\s,]*(~@|[\\[\\]{}()'`~^@]|\"(?:\\\\.|[^\\\\\"])*\"?|;.*|[^\\s\\[\\]{}('\"`,;)]*)";
	static Pattern PATTERN = Pattern.compile(TOKENS_REGEX);
	static Map<String, String> QUOTE_TOKENS_MAP = Map.of("'", "quote", "~", "unquote", "`", "quasiquote",
			"~@", "splice-unquote", "@", "deref");

	List<String> tokens;
	Integer position;
	
	public reader(List<String> tokens) {
		this.tokens = tokens;
		this.position = 0;
	}
	
	public String next() {
		String current = this.peek();
		position++;
		return current;
	}

	public String peek() {
		return tokens.get(position);
	}

	public static types.MalType read_str(String input) {
		// This function will call tokenize and then
		// create a new Reader object instance with the tokens.
		// Then it will call read_form with the Reader instance.
		
		List<String> tokens = tokenize(input);
		reader myReader = new reader(tokens);
		return read_form(myReader);
	}

	public static List<String> tokenize(String input) {
		Matcher matcher = PATTERN.matcher(input);
		List<String> results = new ArrayList<>();
		while (matcher.find()) {
			results.add(matcher.group(1));
		}
		return results;
	}
	
	public static MalType read_form(reader myReader) {
		if ("([{".contains(myReader.peek())) {
			return read_list(myReader);
		} else {
			return read_atom(myReader);
		}
	}

	private static MalType read_atom(reader myReader) {
		String token = myReader.next();
		if (token.matches("\\d+")) {
			Integer value = Integer.parseInt(token);
			return new types().new MalInteger(value);
		} else if (token.startsWith("\"") || token.endsWith("\"")) {
				// A String is a token with quotes "
				
				// A single quote string is not valid
				if (token.length() == 1) {
					throw new RuntimeException("Single quote");
				}
				
				if (!(token.startsWith("\"") && token.endsWith("\""))) {
					throw new RuntimeException("Unbalanced quotes");
				}

				String unescaped = unescape(token.substring(1, token.length() - 1));
				return new types().new MalString(unescaped);

		} else if (QUOTE_TOKENS_MAP.containsKey(token)) {
			// The following form needs to be wrapped on a list with the substitution from the quoteTokens map
			String substitution = QUOTE_TOKENS_MAP.get(token);
			MalType symbol = new types().new MalSymbol(substitution);
			MalType expr = read_form(myReader);
			MalList list = new types().new MalList(Arrays.asList(symbol, expr));
			return list;
		} else if ("^".equals(token)) {
			// metadata on an expr - the next 2 forms are: metadata and value
			MalType withMeta = new types().new MalSymbol("with-meta");
			MalType metadata = read_form(myReader);
			MalType expr = read_form(myReader);
			MalList list = new types().new MalList(Arrays.asList(withMeta, expr, metadata));
			return list;
		} else {
			return new types().new MalSymbol(token);
		}
	}
	
	private static String unescape(String input) {
		StringBuilder builder = new StringBuilder();
		boolean escaping = false;
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);

			if (!escaping && c == '\\') {
				escaping = true;
				continue;
			}
			
			if (!escaping) {
				builder.append(c);
			} else {
				if (c == '\\') {
					builder.append('\\');
					escaping = false;
				} else if (c == 'n') { // TODO: \b \f \r \t
					builder.append('\n');
					escaping = false;
				} else if (c == '\"') {
					builder.append('\"');
					escaping = false;
				} else if (c == '\'') {
					builder.append('\'');
					escaping = false;
				}
			}	
		}
		if (escaping) throw new RuntimeException("Unfinished escape sequence");
		return builder.toString();
	}

	private static MalList read_list(reader myReader) {
		String openToken = myReader.next();		
		String closeToken = ")";
		switch (openToken) {
			case "[":
				closeToken = "]";
				break;
				
			case "{":
				closeToken = "}";
				break;
		}
		
		List<MalType> items = new ArrayList<>();
		while (!closeToken.equals(myReader.peek())) {
			items.add(read_form(myReader));
		}
		myReader.next(); // consume the closing token

		return new types().new MalList(items, openToken, closeToken);
	}

}
