package mal;

import java.util.List;
import java.util.stream.Collectors;

public class types {

	public class MalType { }

	public class MalList extends MalType {
		public List<MalType> items;
	
		public MalList(List<MalType> items) {
			this.items = items;
		}
		
		public String toString() {
			List<String> strItems = items.stream().map(i -> i.toString()).collect(Collectors.toList());
			String joined = String.join(" ", strItems);
			return String.format("(%s)", joined);
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
			String escaped = value.replace("\"", "\\\"");
			return String.format("\"%s\"", escaped);
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
}

