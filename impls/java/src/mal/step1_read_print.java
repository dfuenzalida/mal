package mal;

import mal.types.MalType;

public class step1_read_print {

	public MalType eval(MalType input) {
		return input;
	}

	public String rep(String input) {
		try {
			MalType afterRead = reader.read_str(input);
			MalType afterEval = this.eval(afterRead);
			return printer.pr_str(afterEval);
		} catch (Exception e) {
			return "EOF"; // generic error
		}
	}

	public static void main(String... args) {
		step1_read_print rp = new step1_read_print();

		// Local Test
		// System.out.println(rp.rep("\"abc\""));

		String input;
		do {
			input = System.console().readLine("user> ");
			if (input != null) {
				System.out.println(rp.rep(input));
			}
		} while (input != null);

	}
}
