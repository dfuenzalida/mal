package mal;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import mal.types.MalList;
import mal.types.MalSymbol;
import mal.types.MalType;

public class env {

	public env outer;
	Map<MalSymbol, MalType> data;
	static types malTypes = new types();

	public env(env outer, MalList binds, MalList exprs) {
		this.outer = outer;
		this.data = new HashMap<>();
		for (int i = 0; binds != null && exprs != null && i < Math.min(binds.items.size(), exprs.items.size()); i++) {
			MalSymbol key = (MalSymbol) binds.items.get(i);
			MalType value = exprs.items.get(i);
			this.set(key, value);
		}
	}

	public void set(MalSymbol key, MalType value) {
		data.put(key, value);
	}

	public env find(MalSymbol key) {
		if (data.containsKey(key)) {
			return this;
		} else {
			if (outer == null) {
				throw malTypes.new RepException(String.format("'%s' not found", key.name));
			} else {
				return this.outer.find(key);
			}
		}
	}

	public MalType get(MalSymbol key) {
		env env = this.find(key);
		if (env == null) {
			throw malTypes.new RepException(String.format("'%s' not found", key));
		} else {
			return env.data.get(key);
		}
	}

	public String toString() {
		return String.join(", ",
				data.keySet().stream().map(k -> k.toString()).collect(Collectors.toList()));
	}
}
