package mal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mal.types.MalHashMap;
import mal.types.MalList;
import mal.types.MalType;

public class reader {
	
	static String TOKENS_REGEX = "[\\s,]*(~@|[\\[\\]{}()'`~^@]|\"(?:\\\\.|[^\\\\\"])*\"?|;.*|[^\\s\\[\\]{}('\"`,;)]*)";
	static Pattern PATTERN = Pattern.compile(TOKENS_REGEX);
	static Map<String, String> QUOTE_TOKENS_MAP = Map.of("'", "quote", "~", "unquote", "`", "quasiquote",
			"~@", "splice-unquote", "@", "deref");
	static Map<Character, Character> unescapeMap = Map.of('\\', '\\', 'n', '\n', 'b', '\b', 'f', '\f',
			'r', '\r', 't', '\t', '\"', '\"', '\'', '\'');

	List<String> tokens;
	Integer position;
	static types malTypes = new types();
	
	public reader(List<String> tokens) {
		this.tokens = tokens;
		this.position = 0;
	}
	
	public String next() {
		return tokens.get(position++);
	}

	public String peek() {
		return tokens.get(position);
	}

	public static types.MalType read_str(String input) {
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
		if ("([".contains(myReader.peek())) {
			return read_list(myReader);
		} else if ("{".equals(myReader.peek())) {
			return read_hashmap(myReader);
		} else {
			return read_atom(myReader);
		}
	}

	private static MalType read_atom(reader myReader) {
		String token = myReader.next();
		if (token.matches("-?\\d+")) {
			Integer value = Integer.parseInt(token);
			return malTypes.new MalInteger(value);
		} else if (token.startsWith("\"") || token.endsWith("\"")) {
				// A String is a token with quotes "
				
				// A single quote string is not valid
				if (token.length() == 1) {
					throw malTypes.new MalException(malTypes.new MalString("unbalanced quotes"));
				}
				
				if (!(token.startsWith("\"") && token.endsWith("\""))) {
					throw malTypes.new MalException(malTypes.new MalString("unbalanced quotes"));
				}

				String unescaped = unescape(token.substring(1, token.length() - 1));
				return malTypes.new MalString(unescaped);
		} else if (QUOTE_TOKENS_MAP.containsKey(token)) {
			// The following form needs to be wrapped on a list with the substitution from the quoteTokens map
			String substitution = QUOTE_TOKENS_MAP.get(token);
			MalType symbol = malTypes.new MalSymbol(substitution);
			MalType expr = read_form(myReader);
			MalList list = malTypes.new MalList(Arrays.asList(symbol, expr));
			return list;
		} else if ("^".equals(token)) {
			// metadata on an expr - the next 2 forms are: metadata and value
			MalType withMeta = malTypes.new MalSymbol("with-meta");
			MalType metadata = read_form(myReader);
			MalType expr = read_form(myReader);
			MalList list = malTypes.new MalList(Arrays.asList(withMeta, expr, metadata));
			return list;
		} else if ("nil".equals(token)) {
			return types.MalNil;
		} else if (token.startsWith(";")) {
			return types.MalComment; // special value that won't print a result in the main loop
		} else if ("true".equals(token)) {
			return types.MalTrue;
		} else if ("false".equals(token)) {
			return types.MalFalse;
		} else if (token.startsWith(":")) {
			if (token.length() < 2) throw malTypes.new MalException(malTypes.new MalString("Invalid keyword name"));
			return malTypes.new MalKeyword(token.substring(1));
		} else {
			return malTypes.new MalSymbol(token);
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
			} else if (unescapeMap.containsKey(c)) {
				builder.append(unescapeMap.get(c));
				escaping = false;
			}
		}

		if (escaping) throw malTypes.new MalException(malTypes.new MalString("Unexpected end of input"));
		return builder.toString();
	}

	private static MalList read_list(reader myReader) {
		try {
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
				MalType item = read_form(myReader);
				if (item != types.MalComment) {
					items.add(item);
				}
			}
			myReader.next(); // consume the closing token

			return malTypes.new MalList(items, openToken, closeToken);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw malTypes.new MalException(malTypes.new MalString("EOF"));
		}
	}

	private static MalType read_hashmap(reader myReader) {
		myReader.next(); // drop the open bracket
		String closeToken = "}";
		List<MalType> items = new ArrayList<>();
		while (!closeToken.equals(myReader.peek())) {
			MalType item = read_form(myReader);
			if (item != types.MalComment) {
				items.add(item);
			}
		}
		myReader.next(); // consume the closing token

		// This gets actually eval'd in step_X#eval_ast
		MalHashMap result = malTypes.new MalHashMap(Collections.emptyMap());
		for (Integer i = 0; i < items.size() - 1; i += 2) {
			MalType key = items.get(i);
			MalType val = items.get(i+1);
			result.pairs.put(key, val);
		}
		return result;
	}
}
