package mal;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mal.types.MalList;
import mal.types.MalType;

public class reader {
	
	static String TOKENS_REGEX = "[\\s,]*(~@|[\\[\\]{}()'`~^@]|\"(?:\\\\.|[^\\\\\"])*\"?|;.*|[^\\s\\[\\]{}('\"`,;)]*)";
	static Pattern pattern = Pattern.compile(TOKENS_REGEX);

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
		Matcher matcher = pattern.matcher(input);
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
		} else {
			// A String is a token with quotes "
			if (token.startsWith("\"") || token.endsWith("\"")) {
				
				// A single quote string is not valid
				if (token.length() == 1) {
					throw new RuntimeException("Single quote");
				}
				
				if (!(token.startsWith("\"") && token.endsWith("\""))) {
					throw new RuntimeException("Unbalanced quotes");
				}

				// Just to check properly escaped tokens for now
				String unescaped = unescape(token.substring(1, token.length() - 1));

				return new types().new MalSymbol(token);
			} else {
				return new types().new MalSymbol(token);
			}
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
				} else if (c == 'b') {
					builder.append('\b');
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
		switch(openToken) {
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

		return new types().new MalList(items);
	}

//	public static void main(String[] args) {
//		System.out.println(tokenize("123"));
//	}
}
