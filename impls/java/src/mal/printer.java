package mal;

import mal.types.MalType;

public class printer {

	public static String pr_str(MalType input, boolean print_readably) {
		return input.toString(print_readably);
	}
}
