package mal;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class types {

	static Map<Character, String> ESCAPE_CHARS = Map.of('\\', "\\\\", '\n', "\\n", '\r', "\\r", '\t', "\\t", '"', "\\\"");

	public class MalType { }

	public static MalType MalNil = new types().new MalType() {
		public String toString() {
			return "nil";
		}
	};

	public class MalList extends MalType {
		public List<MalType> items;
		public String open;
		public String close;
	
		public MalList(List<MalType> items) {
			this.items = items;
			this.open = "(";
			this.close = ")";
		}

		public MalList(List<MalType> items, String open, String close) {
			this.items = items;
			this.open = open;
			this.close = close;
		}

		public String toString() {
			List<String> strItems = items.stream().map(i -> i.toString()).collect(Collectors.toList());
			String joined = String.join(" ", strItems);
			return String.format("%s%s%s", open, joined, close);
		}
	}
	
	public class MalInteger extends MalType {
		public Integer value;
		public MalInteger(Integer value) {
			this.value = value;
		}

		public String toString() {
			return value.toString();
		}
	}

	public class MalString extends MalType {
		public String value;
		public MalString(String value) {
			this.value = value;
		}

		public String toString() {
			StringBuilder builder = new StringBuilder();
			for (int i=0; i < value.length(); i++) {
				char c = value.charAt(i);
				builder.append(ESCAPE_CHARS.containsKey(c) ? ESCAPE_CHARS.get(c) : c);
			}
			return String.format("\"%s\"", builder.toString());
		}
	}

	public class MalKeyword extends MalType {
		public String name;
		public MalKeyword(String name) {
			this.name = name;
		}

		public String toString() {
			return String.format(":%s", name.toString());
		}
	}

	public class MalSymbol extends MalType {
		public String name;
		public MalSymbol(String name) {
			this.name = name;
		}

		public String toString() {
			return name.toString();
		}
	}

	public class MalBoolean extends MalType {
		public boolean value;
		MalBoolean(boolean value) {
			this.value = value;
		}

		public String toString() {
			return value ? "true":"false";
		}
	}

	public static MalBoolean MalTrue  = new types().new MalBoolean(true);
	public static MalBoolean MalFalse = new types().new MalBoolean(false);


	public abstract class MalFunction extends MalType {
		abstract MalType apply(MalList args);
	}
}

