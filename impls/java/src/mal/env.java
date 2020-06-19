package mal;

import java.util.HashMap;
import java.util.Map;

import mal.types.MalSymbol;
import mal.types.MalType;

public class env {

	public env outer;
	Map<MalSymbol, MalType> data;

	public env(env outer) {
		this.outer = outer;
		this.data = new HashMap<>();
	}

	public void set(MalSymbol key, MalType value) {
		data.put(key, value);
	}

	public env find(MalSymbol key) {
		if (data.containsKey(key)) {
			return this;
		} else {
			if (outer == null) {
				throw new RepException(String.format("!'%s' not found on any environment", key.name));
			} else {
				return this.outer.find(key);
			}
		}
	}

	public MalType get(MalSymbol key) {
		env env = this.find(key);
		if (env == null) {
			throw new RuntimeException(String.format("'%s' not found in any environment", key));
		} else {
			return env.data.get(key);
		}
	}

	// Exceptions of this type have their message printed in the REPL instead of just 'EOF'
	public class RepException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public RepException(String msg) {
			super(msg);
		}
	}
}
