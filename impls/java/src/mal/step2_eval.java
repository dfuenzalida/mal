package mal;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import mal.types.MalFunction;
import mal.types.MalInteger;
import mal.types.MalList;
import mal.types.MalSymbol;
import mal.types.MalType;

public class step2_eval {
	mal.types malTypes = new types();

	Map<String, MalType> repl_env = Map.of(
			"+", malTypes.new MalFunction() {
				MalType apply(MalList args) {
					MalInteger arg0 = (MalInteger) args.items.get(0);
					MalInteger arg1 = (MalInteger) args.items.get(1);
					return malTypes.new MalInteger(arg0.value + arg1.value);
				}
			},
			"-", malTypes.new MalFunction() {
				MalType apply(MalList args) {
					MalInteger arg0 = (MalInteger) args.items.get(0);
					MalInteger arg1 = (MalInteger) args.items.get(1);
					return malTypes.new MalInteger(arg0.value - arg1.value);
				}
			},
			"*", malTypes.new MalFunction() {
				MalType apply(MalList args) {
					MalInteger arg0 = (MalInteger) args.items.get(0);
					MalInteger arg1 = (MalInteger) args.items.get(1);
					return malTypes.new MalInteger(arg0.value * arg1.value);
				}
			},
			"/", malTypes.new MalFunction() {
				MalType apply(MalList args) {
					MalInteger arg0 = (MalInteger) args.items.get(0);
					MalInteger arg1 = (MalInteger) args.items.get(1);
					return malTypes.new MalInteger((int) arg0.value / arg1.value);
				}
			});

	public MalType eval(MalType input, Map<String, MalType> env) {
		if (!(input instanceof MalList)) {
			return eval_ast(input, env);
		} else {
			MalList inputList = (MalList) input;
			if (inputList.items.isEmpty()) {
				return input;
			} else {
				MalList evaluated = (MalList) eval_ast(inputList, env);
				if ("(".equals(inputList.open)) { // actual list, not vector or map
					MalFunction func = (MalFunction) evaluated.items.get(0);
					MalList otherArgs = malTypes.new MalList(evaluated.items.subList(1, evaluated.items.size()));
					return func.apply(otherArgs);
				} else {
					MalList result = malTypes.new MalList(evaluated.items);
					result.open = inputList.open;
					result.close = inputList.close;
					return result;
				}
			}
		}
	}

	public MalType eval_ast(MalType ast, Map<String, MalType> env) {
		if (ast instanceof MalSymbol) {
			MalSymbol astSymbol = (MalSymbol) ast;
			if (env.containsKey(astSymbol.name)) {
				return env.get(astSymbol.name);
			} else {
				throw new RuntimeException("Unknown name: "+ astSymbol.name);
			}
		} else if (ast instanceof MalList) {
			MalList args = (MalList) ast;
			List<MalType> evaluated = args.items.stream().map(arg -> eval(arg, env)).collect(Collectors.toList());
			return malTypes.new MalList(evaluated);
		} else {
			return ast;
		}
	}

	public String rep(String input) {
		try {
			MalType afterRead = reader.read_str(input);
			MalType afterEval = this.eval(afterRead, repl_env);
			return printer.pr_str(afterEval, true);
		} catch (Exception e) {
			return "EOF"; // generic error
		}
	}

	public static void main(String... args) {
		step2_eval rp = new step2_eval();

		// Local Test
		// System.out.println(rp.rep("[1 2 (+ 1 2)]"));

		String input;
		do {
			input = System.console().readLine("user> ");
			if (input != null) {
				System.out.println(rp.rep(input));
			}
		} while (input != null);
	}
}
