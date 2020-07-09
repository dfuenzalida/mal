package mal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class types {

	static Map<Character, String> ESCAPE_CHARS = Map.of('\\', "\\\\", '\n', "\\n", '\r', "\\r", '\t', "\\t", '"', "\\\"");

	public abstract class MalType {
		public abstract String toString(boolean print_readably);
		public abstract int hashCode();

		public String toString() {
			return this.toString(true);
		}
	}

	public static MalType MalComment = new types().new MalType() {
		public String toString(boolean print_readably) {
			return "";
		}

		public int hashCode() {
			return 0;
		}
	};

	public static MalType MalNil = new types().new MalType() {
		public String toString(boolean print_readably) {
			return "nil";
		}

		public boolean equals(Object obj) {
			return (obj == types.MalNil);
		}

		public int hashCode() {
			return 1;
		}
	};

	public class MalList extends MalType {
		public List<MalType> items;
		public String open;
		public String close;
	
		public MalList(Collection<MalType> items) {
			this.items = new ArrayList<>(items);
			this.open = "(";
			this.close = ")";
		}

		public MalList(List<MalType> items, String open, String close) {
			this.items = items;
			this.open = open;
			this.close = close;
		}

		public MalType nth(Integer index) {
			return this.items.get(index);
		}

		public String toString(boolean print_readably) {
			List<String> strItems = items.stream().map(i -> i.toString(print_readably)).collect(Collectors.toList());
			String joined = String.join(" ", strItems);
			return String.format("%s%s%s", open, joined, close);
		}

		public boolean equals(Object obj) {
			if (!(obj instanceof MalList)) return false;
			MalList objList = (MalList) obj;
			if (objList.items.size() != this.items.size()) return false;
			for (int i = 0; i < this.items.size(); i++) {
				if (!this.items.get(i).equals(objList.items.get(i))) {
					return false;
				}
			}
			return true;
		}

		public int hashCode() {
			return this.items.hashCode();
		}
	}
	
	public class MalInteger extends MalType {
		public Integer value;
		public MalInteger(Integer value) {
			this.value = value;
		}

		public String toString(boolean print_readably) {
			return value.toString();
		}

		public boolean equals(Object obj) {
			return obj instanceof MalInteger && ((MalInteger)obj).value.equals(this.value);
		}

		public int hashCode() {
			return this.value.hashCode();
		}
	}

	public class MalString extends MalType {
		public String value;
		public MalString(String value) {
			this.value = value;
		}

		public String toString(boolean print_readably) {
			if (print_readably) {
				StringBuilder builder = new StringBuilder();
				for (int i=0; i < value.length(); i++) {
					char c = value.charAt(i);
					builder.append(ESCAPE_CHARS.containsKey(c) ? ESCAPE_CHARS.get(c) : c);
				}
				return String.format("\"%s\"", builder.toString());
			} else {
				return value;
			}
		}

		public int hashCode() {
			return value.hashCode();
		}

		public boolean equals(Object obj) {
			return obj instanceof MalString && ((MalString)obj).value.equals(this.value);
		}
	}

	public class MalKeyword extends MalType {
		public String name;
		public MalKeyword(String name) {
			this.name = name;
		}

		public String toString(boolean print_readably) {
			return String.format(":%s", name);
		}

		public int hashCode() {
			return this.toString().hashCode();
		}

		public boolean equals(Object obj) {
			return obj instanceof MalKeyword && ((MalKeyword)obj).name.equals(this.name);
		}
	}

	public class MalSymbol extends MalType {
		public String name;
		public MalSymbol(String name) {
			this.name = name;
		}

		public String toString(boolean print_readably) {
			return name.toString();
		}

		public boolean equals(Object obj) {
			return (obj instanceof MalSymbol) && ((MalSymbol)obj).name.equals(this.name);
		}

		public int hashCode() { // important for Map lookups to actually work
			return name.hashCode();
		}

	}

	public class MalBoolean extends MalType {
		public Boolean value;
		MalBoolean(boolean value) {
			this.value = value;
		}

		public String toString(boolean print_readably) {
			return value ? "true":"false";
		}

		public boolean equals(Object obj) {
			return (obj instanceof MalBoolean) && ((MalBoolean) obj).value == this.value;
		}

		public int hashCode() {
			return this.value.hashCode();
		}
	}

	public static MalBoolean MalTrue  = new types().new MalBoolean(true);
	public static MalBoolean MalFalse = new types().new MalBoolean(false);


	public abstract class MalFunction extends MalType {
		public boolean is_macro = false;

		abstract MalType apply(MalList args);

		public String toString(boolean print_readably) {
			return "#<function>";
		}

		public int hashCode() {
			return 16384;
		}
	}

	// Exceptions of this type have their message printed in the REPL instead of just 'EOF'
	public class RepException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public RepException(String msg) {
			super(msg);
		}
	}

	public class FunctionTco extends MalType {
		public MalType ast;
		public MalList params;
		public env functionEnv;
		public MalFunction fn;
		public boolean is_macro = false;

		public FunctionTco(MalType ast, MalList params, env functionEnv, MalFunction fn) {
			this.ast = ast;
			this.params = params;
			this.functionEnv = functionEnv;
			this.fn = fn;
		}

		public String toString(boolean print_readably) {
			return "#<functionTco>";
		}

		public int hashCode() {
			return ast.hashCode() ^ params.hashCode() ^ fn.hashCode();
		}
	}

	public class MalAtom extends MalType {
		MalType value;

		public MalAtom(MalType value) {
			this.value = value;
		}

		public String toString(boolean print_readably) {
			return String.format("(atom %s)", value.toString(print_readably));
		}

		public String toString() {
			return this.toString(true);
		}

		public int hashCode() {
			return this.value.hashCode() + 1;
		}
	}

	public class MalException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public MalType value;
		public MalException(MalType value) {
			this.value = value;
		}
	}

	public class MalHashMap extends MalType {
		public HashMap<MalType, MalType> pairs = new HashMap<>();

		public MalHashMap(Map<MalType, MalType> kvs) {
			pairs.putAll(kvs);
		}

		public boolean equals(Object obj) {
			return (obj instanceof MalHashMap) && this.pairs.equals(((MalHashMap) obj).pairs);
		}

		public String toString() {
			return toString(true);
		}

		public String toString(boolean readably) {
			StringBuffer output = new StringBuffer();
			output.append("{");
			List<String> printedPairs = pairs.entrySet().stream()
				.map(e -> String.format("%s %s", e.getKey().toString(readably), e.getValue().toString(readably)))
				.collect(Collectors.toList());
			output.append(String.join(" ", printedPairs));
			output.append("}");
			return output.toString();
		}


		public int hashCode() {
			return pairs.hashCode();
		}
	}
}