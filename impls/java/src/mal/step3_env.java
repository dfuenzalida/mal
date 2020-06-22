package mal;

import java.util.List;
import java.util.stream.Collectors;

import mal.types.MalFunction;
import mal.types.MalInteger;
import mal.types.MalList;
import mal.types.MalSymbol;
import mal.types.MalType;
import mal.types.RepException;

public class step3_env {

	mal.types malTypes = new types();
	env repl_env = new env(null, null, null);
	
	public step3_env() {
		repl_env.set(
				malTypes.new MalSymbol("+"),
				malTypes.new MalFunction() {
					MalType apply(MalList args) {
					MalInteger arg0 = (MalInteger) args.items.get(0);
					MalInteger arg1 = (MalInteger) args.items.get(1);
					return malTypes.new MalInteger(arg0.value + arg1.value);
				}
			});

		repl_env.set(
				malTypes.new MalSymbol("-"),
				malTypes.new MalFunction() {
					MalType apply(MalList args) {
					MalInteger arg0 = (MalInteger) args.items.get(0);
					MalInteger arg1 = (MalInteger) args.items.get(1);
					return malTypes.new MalInteger(arg0.value - arg1.value);
				}
			});

		repl_env.set(
				malTypes.new MalSymbol("*"),
				malTypes.new MalFunction() {
					MalType apply(MalList args) {
					MalInteger arg0 = (MalInteger) args.items.get(0);
					MalInteger arg1 = (MalInteger) args.items.get(1);
					return malTypes.new MalInteger(arg0.value * arg1.value);
				}
			});

		repl_env.set(
				malTypes.new MalSymbol("/"),
				malTypes.new MalFunction() {
					MalType apply(MalList args) {
					MalInteger arg0 = (MalInteger) args.items.get(0);
					MalInteger arg1 = (MalInteger) args.items.get(1);
					return malTypes.new MalInteger(arg0.value / arg1.value);
				}
			});
	}


	public MalType eval(MalType input, env replEnv) {
		if (!(input instanceof MalList)) {
			return eval_ast(input, replEnv);
		} else {
			MalList inputList = (MalList) input;
			if (inputList.items.isEmpty()) {
				return input;
			} else {
				if ("(".equals(inputList.open)) { // actual list, not vector or map
					MalType firstUnevaluated = inputList.items.get(0);
					if (firstUnevaluated instanceof MalSymbol) {
						MalSymbol firstSymbol = (MalSymbol) firstUnevaluated;
						if (firstSymbol.name.equals("def!")) {
							MalSymbol key = (MalSymbol) inputList.items.get(1);
							MalType value = inputList.items.get(2);
							MalType valueEvaled = eval(value, replEnv);
							replEnv.set(key, valueEvaled);
							return valueEvaled;
						} else if (firstSymbol.name.equals("let*")) {
							// Create new env
							env newEnv = new env(replEnv, null, null);
							// Eval the key-value pairs in the new env
							MalList keyValues = (MalList) inputList.items.get(1);
							for (int i = 0; i < keyValues.items.size(); i += 2) {
								MalSymbol key = (MalSymbol) keyValues.items.get(i);
								MalType value = keyValues.items.get(i + 1);
								MalType valueEvaled = eval(value, newEnv);
								newEnv.set(key, valueEvaled);
							}							
							// finally, eval the body after the name-value pairs and return the result
							MalType letBody = inputList.items.get(2);
							return eval(letBody, newEnv);
						}
					}
					
					MalList evaluated = (MalList) eval_ast(inputList, replEnv);
					MalFunction func = (MalFunction) evaluated.items.get(0);
					MalList otherArgs = malTypes.new MalList(evaluated.items.subList(1, evaluated.items.size()));
					return func.apply(otherArgs);
				} else {
					MalList evaluated = (MalList) eval_ast(inputList, replEnv);
					MalList result = malTypes.new MalList(evaluated.items);
					result.open = inputList.open;
					result.close = inputList.close;
					return result;
				}
			}
		}
	}

	public MalType eval_ast(MalType ast, env replEnv) {
		if (ast instanceof MalSymbol) {
			MalSymbol astSymbol = (MalSymbol) ast;
			return replEnv.get(astSymbol);
		} else if (ast instanceof MalList) {
			MalList args = (MalList) ast;
			List<MalType> evaluated = args.items.stream().map(arg -> eval(arg, replEnv)).collect(Collectors.toList());
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
		} catch (RepException rex) {
			return rex.getMessage();
		} catch (Exception e) {
			return "EOF"; // generic error
		}
	}

	public static void main(String... args) {
		step3_env rp = new step3_env();

		// Local Test
		// System.out.println(rp.rep("(def! a 6)"));

		String input;
		do {
			input = System.console().readLine("user> ");
			if (input != null) {
				System.out.println(rp.rep(input));
			}
		} while (input != null);
	}
}
