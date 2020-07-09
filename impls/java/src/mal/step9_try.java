package mal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import mal.types.FunctionTco;
import mal.types.MalException;
import mal.types.MalFunction;
import mal.types.MalList;
import mal.types.MalSymbol;
import mal.types.MalType;
import mal.types.RepException;

public class step9_try {

	static mal.types malTypes = new types();
	static env repl_env = new env(null, null, null);
	
	public step9_try() {
		core.ns.forEach((name, fn) -> repl_env.set(malTypes.new MalSymbol(name), fn));
	}

	public static MalType eval(MalType ast, env replEnv) {
		while (true) {
			if (!(ast instanceof MalList)) {
				return eval_ast(ast, replEnv);
			} else {
				ast = macroexpand(ast, replEnv);
				if (!(ast instanceof MalList)) {
					return eval_ast(ast, replEnv);
				}
				MalList inputList = (MalList) ast;
				// Remove the instances of MalComment
				inputList = removeComments(inputList);
				if (inputList.items.isEmpty()) {
					return ast;
				} else {
					if ("(".equals(inputList.open)) { // actual list, not vector or map
						MalType firstUnevaluated = inputList.nth(0);
						if (firstUnevaluated instanceof MalSymbol) {
							MalSymbol firstSymbol = (MalSymbol) firstUnevaluated;
							if (firstSymbol.name.equals("def!")) {
								MalSymbol key = (MalSymbol) inputList.nth(1);
								MalType value = inputList.nth(2);
								MalType valueEvaled = eval(value, replEnv);
								replEnv.set(key, valueEvaled);
								return valueEvaled;
							} else if (firstSymbol.name.equals("defmacro!")) {
								MalSymbol key = (MalSymbol) inputList.nth(1);
								MalType value = inputList.nth(2);
								FunctionTco macro = (FunctionTco) eval(value, replEnv);
								macro.is_macro = true;
								replEnv.set(key, macro);
								return macro;
							} else if (firstSymbol.name.equals("let*")) {
								// Create new env
								env newEnv = new env(replEnv, null, null);
								// Eval the key-value pairs in the new env
								MalList keyValues = (MalList) inputList.nth(1);
								for (int i = 0; i < keyValues.items.size(); i += 2) {
									MalSymbol key = (MalSymbol) keyValues.nth(i);
									MalType value = keyValues.nth(i + 1);
									MalType valueEvaled = eval(value, newEnv);
									newEnv.set(key, valueEvaled);
								}							
								// finally, eval the body after the name-value pairs and return the result
								MalType letBody = inputList.nth(2);
								replEnv = newEnv;
								ast = letBody;
								continue;
							} else if (firstSymbol.name.equals("do")) {
								// Evaluate all the elements of the list using eval_ast and return the final evaluated element
								for (int i = 1; i < inputList.items.size() - 1; i++) {
									// TODO eval_ast or just eval? See: (do (/ 1 0) (+ 1 1)) should crash
									eval(inputList.nth(i), replEnv);
								}
								ast = inputList.nth(inputList.items.size() - 1);
								continue;
							} else if (firstSymbol.name.equals("if")) {
								MalType test = eval(inputList.nth(1), replEnv);
								if (types.MalNil.equals(test) || types.MalFalse.equals(test)) {
									if (inputList.items.size() == 3) {
										return types.MalNil;
									} else {
										ast = inputList.nth(3);
										continue;
									}
								} else {
									ast = inputList.nth(2);
									continue;
								}
							} else if (firstSymbol.name.equals("fn*")) {
								final env replEnvCopy = replEnv; // new env(replEnv, null, null); // required because Java wants replEnv to be final
								MalList binds = (MalList) inputList.nth(1);
								MalType expr = inputList.nth(2);
								MalFunction fn = malTypes.new MalFunction() {
									MalType apply(MalList args) {
										MalList[] bindsArgs = bindArgs(binds, args);
										env newEnv = new env(replEnvCopy, bindsArgs[0], bindsArgs[1]);
										return eval(expr, newEnv);
									}
								};
								return malTypes.new FunctionTco(expr, binds, replEnv, fn);
							} else if (firstSymbol.name.equals("quote")) {
								MalType astArg = inputList.nth(1);
								return astArg;
							} else if (firstSymbol.name.equals("quasiquote")) {
								MalType astArg = inputList.nth(1);
								ast = quasiquote(astArg);
							} else if (firstSymbol.name.equals("macroexpand")) {
								MalType astArg = inputList.nth(1);
								return macroexpand(astArg, replEnv);
							} else if (firstSymbol.name.equals("try*")) {
								MalType tryExpr = inputList.nth(1);
								try {
									MalType tryResult = eval(tryExpr, replEnv);
									return tryResult;
								} catch (MalException mex) {
									MalType cause = mex.value;
									if (inputList.items.size() < 3) throw mex; // if catch* is missing
									MalList catchBlock = (MalList) inputList.nth(2);
									// TODO check that catchBlock.nth(0) is the MalSymbol "catch*"
									MalSymbol exName = (MalSymbol) catchBlock.nth(1);
									MalType catchExpr = catchBlock.nth(2);
									env newEnv = new env(replEnv, null, null);
									newEnv.set(exName, cause);
									return eval(catchExpr, newEnv);
								}
							} else {
								// regular function application
								MalList evaluated = (MalList) eval_ast(inputList, replEnv);
								MalType funcOrFunctionTco = evaluated.nth(0);
								MalList funcArgs = malTypes.new MalList(evaluated.items.subList(1, evaluated.items.size()));
								if (funcOrFunctionTco instanceof MalFunction) {
									MalFunction func = (MalFunction) funcOrFunctionTco;
									return func.apply(funcArgs);
								} else {
									FunctionTco funcTco = (FunctionTco) funcOrFunctionTco;
									ast = funcTco.ast;
									MalList funcTcoParams = funcTco.params;
									MalList[] bindsArgs = bindArgs(funcTcoParams, funcArgs);
									replEnv = new env(funcTco.functionEnv, bindsArgs[0], bindsArgs[1]);
									continue;
								}
							}
						} else {
							// inline function application case, eg. "((fn* () 1))"
							if (firstUnevaluated instanceof MalList) {
								MalList firstList = (MalList) firstUnevaluated;
								MalType firstEvaluated = eval(firstList, replEnv);
								List<MalType> updated = new ArrayList<>();
								updated.add(firstEvaluated);
								updated.addAll(inputList.items.subList(1, inputList.items.size()));
								return eval(malTypes.new MalList(updated), replEnv);
							} else if (firstUnevaluated instanceof FunctionTco) {
								FunctionTco funcTco = (FunctionTco) firstUnevaluated;
								MalList funcArgs = malTypes.new MalList(inputList.items.subList(1, inputList.items.size()));
								ast = funcTco.ast;
								MalList funcTcoParams = funcTco.params;
								MalList[] bindsArgs = bindArgs(funcTcoParams, funcArgs);
								replEnv = new env(funcTco.functionEnv, bindsArgs[0], bindsArgs[1]);
								continue;
							}
							// it's a list of Symbols, numbers or strings ... just return it?
							return ast;
						}
					} else {
						// vector and pseudo-maps literals evaluate the contents with eval_ast and wrap in square/braces
						MalList evaluated = (MalList) eval_ast(inputList, replEnv);
						MalList result = malTypes.new MalList(evaluated.items);
						result.open = inputList.open;
						result.close = inputList.close;
						return result;
					}
				}
			}
		}
	}

	private static MalList removeComments(MalList inputList) {
		List<MalType> actualItems = inputList.items.stream()
				.filter(it -> it != types.MalComment)
				.collect(Collectors.toList());
		inputList.items = actualItems;
		return inputList;
	}

	private static MalList[] bindArgs(MalList binds, MalList args) {
		// Copy the lists binds and args lists around, if one of the 'binds' is '&'
		// capture every args afterwards as a list
		MalList bindsNew = malTypes.new MalList(new ArrayList<>());
		MalList argsNew = malTypes.new MalList(new ArrayList<>());
		for (int i = 0; i < binds.items.size(); i++) {
			MalSymbol bindsi = (MalSymbol) binds.nth(i);
			MalType argsi = (i < args.items.size()) ? args.nth(i) : types.MalNil;
			if (bindsi.name.equals("&")) {
				bindsi = (MalSymbol) binds.nth(i + 1); // & more
				if (i < args.items.size()) {
					argsi = malTypes.new MalList(args.items.subList(i, args.items.size()));
				} else {
					argsi = malTypes.new MalList(new ArrayList<>());
				}
				bindsNew.items.add(bindsi);
				argsNew.items.add(argsi);
				break;
			} else {
				bindsNew.items.add(bindsi);
				argsNew.items.add(argsi);
			}
		}
		return new MalList[] { bindsNew, argsNew };
	}

	private static boolean is_pair(MalType param) { // is a non-empty list
		return param instanceof MalList && ((MalList)param).items.size() > 0;
	}

	private static MalType quasiquote(MalType astArg) {
		if (!is_pair(astArg)) { // case i
			List<MalType> items = new ArrayList<>();
			items.add(malTypes.new MalSymbol("quote"));
			items.add(astArg);
			return malTypes.new MalList(items);
		} else {
			MalList astList = (MalList) astArg;

			// case ii
			if (astList.nth(0) instanceof MalSymbol) {
				MalSymbol first = (MalSymbol) astList.nth(0);
				if (first.name.equals("unquote")) {
					return astList.nth(1);
				}
			}

			// case iii
			if (is_pair(astList.nth(0))) {
				MalList firstList = (MalList) astList.nth(0);
				MalType firstFirst = firstList.nth(0);
				if (firstFirst instanceof MalSymbol && ((MalSymbol)firstFirst).name.equals("splice-unquote")) {
					List<MalType> result = new ArrayList<>();
					result.add(malTypes.new MalSymbol("concat"));
					result.add(firstList.nth(1));

					List<MalType> secondThroughLast = new ArrayList<>();
					secondThroughLast.addAll(astList.items.subList(1, firstList.items.size()));
					result.add(quasiquote(malTypes.new MalList(secondThroughLast)));

					return malTypes.new MalList(result);
				}
			}

			// case iv - default if none of the above matched
			List<MalType> result = new ArrayList<>();
			result.add(malTypes.new MalSymbol("cons"));

			result.add(quasiquote(astList.nth(0)));

			List<MalType> secondThroughLast = new ArrayList<>();
			secondThroughLast.addAll(astList.items.subList(1, astList.items.size()));
			result.add(quasiquote(malTypes.new MalList(secondThroughLast)));

			return malTypes.new MalList(result);
		}
	}

	private static boolean is_macro_call(MalType ast, env replEnv) {
		if (ast instanceof MalList) {
			MalList astList = (MalList) ast;
			if (!astList.items.isEmpty()) {
				MalType first = astList.nth(0);
				if (first instanceof MalSymbol) {
					try {
						MalType resolved = replEnv.get((MalSymbol) first);
						if (resolved instanceof FunctionTco) {
							return ((FunctionTco)resolved).is_macro;
						}

						if (resolved instanceof MalFunction) {
							return ((MalFunction)resolved).is_macro;
						}
					} catch (Exception ex) {
						return false;
					}
				}
			}
		}
		return false;
	}

	private static MalType macroexpand(MalType ast, env replEnv) {
		boolean is_macro = is_macro_call(ast, replEnv);
		while (is_macro) {
			MalList astList = (MalList) ast;
			FunctionTco fnTco = (FunctionTco) replEnv.get((MalSymbol) astList.nth(0));
			MalFunction fn = fnTco.fn;
			List<MalType> fnArgs = new ArrayList<>();
			fnArgs.addAll(astList.items.subList(1, astList.items.size()));
			ast = fn.apply(malTypes.new MalList(fnArgs));
			is_macro = is_macro_call(ast, replEnv); // maybe fnTco.functionEnv ?
		}
		return ast;
	}

	public static MalType eval_ast(MalType ast, env replEnv) {
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
			MalType afterEval = eval(afterRead, repl_env);
			if (afterEval == types.MalComment) {
				return null; // don't print a result in the main loop
			} else {
				return printer.pr_str(afterEval, true);
			}
		} catch (types.MalException mex) {
			return "Exception: " + mex.value.toString(false);
		} catch (RepException rex) {
			return rex.getMessage();
		} catch (Exception e) {
			return "EOF"; // generic error
		}
	}

	public static void main(String... args) {
		step9_try rp = new step9_try();

		// Define `eval`
		MalFunction evalFn = malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalType ast = args.nth(0);
				return eval(ast, repl_env);
			}
		};

		repl_env.data.put(malTypes.new MalSymbol("eval"), evalFn);

		// Functions defined in MAL itself
		rp.rep("(def! not (fn* (a) (if a false true)))"); // (not <expr>)
		rp.rep("(def! load-file (fn* (f) (eval (read-string (str \"(do \" (slurp f) \"\\nnil)\")))))");
		rp.rep("(defmacro! cond (fn* (& xs) (if (> (count xs) 0) (list 'if (first xs) (if (> (count xs) 1) (nth xs 1) (throw \"odd number of forms to cond\")) (cons 'cond (rest (rest xs)))))))");

		// If there's command line arguments:
		// - load the the args after the first as *ARGV*
		List<String> quotedArgs = Collections.emptyList();
		if (args.length > 1) {
			quotedArgs = Arrays.asList(args).subList(1, args.length).stream()
					.map(a -> String.format("\"%s\"", a))
					.collect(Collectors.toList());
		}
		String defArgv = String.format("(def! *ARGV* (list %s))", String.join(" ", quotedArgs));
		rp.rep(defArgv);

		// use the first one as a script name to load with load-file, then exit
		if (args.length > 0) {
			String fileName = args[0];
			rp.rep(String.format("(load-file \"%s\")", fileName));
			System.exit(0); // terminate execution after loading the script
		}

		// Local Tests

		// Main loop
		String input;
		do {
			input = System.console().readLine("user> ");
			if (input != null) {
				String repResult = rp.rep(input);
				if (repResult != null) {
					System.out.println(repResult);
				}
			}
		} while (input != null);
	}
}
