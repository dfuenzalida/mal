package mal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import mal.types.FunctionTco;
import mal.types.MalFunction;
import mal.types.MalList;
import mal.types.MalSymbol;
import mal.types.MalType;
import mal.types.RepException;

public class step7_quote {

	static mal.types malTypes = new types();
	static env repl_env = new env(null, null, null);
	
	public step7_quote() {
		core.ns.forEach((name, fn) -> repl_env.set(malTypes.new MalSymbol(name), fn));
	}

	public static MalType eval(MalType ast, env replEnv) {
		while (true) {
			if (!(ast instanceof MalList)) {
				return eval_ast(ast, replEnv);
			} else {
				MalList inputList = (MalList) ast;
				// Remove the instances of MalComment
				List<MalType> actualItems = inputList.items.stream()
						.filter(it -> it != types.MalComment)
						.collect(Collectors.toList());
				inputList.items = actualItems;
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
										env newEnv = new env(replEnvCopy, bindsNew, argsNew);
										return eval(expr, newEnv);
									}
								};
								return malTypes.new FunctionTco(expr, binds, replEnv, fn);
							} else if (firstSymbol.name.equals("quote")) {
								MalType astArg = inputList.nth(1);
								return astArg;
							} else if (firstSymbol.name.equals("quasiquote")) {
								MalType astArg = inputList.nth(1);
								if (!is_pair(astArg)) { // case i
									List<MalType> items = new ArrayList<>();
									items.add(malTypes.new MalSymbol("quote"));
									items.add(astArg);
									ast = malTypes.new MalList(items);
									continue;
								} else {
									MalList astList = (MalList) astArg;

									// case ii
									if (astList.nth(0) instanceof MalSymbol) {
										MalSymbol first = (MalSymbol) astList.nth(0);
										if (first.name.equals("unquote")) {
											ast = astList.nth(1);
											continue;
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
											secondThroughLast.add(malTypes.new MalSymbol("quasiquote"));
											secondThroughLast.addAll(firstList.items.subList(1, firstList.items.size()));
											MalType secThroughLastRes = eval(malTypes.new MalList(secondThroughLast), replEnv);
											result.add(secThroughLastRes);
											ast = malTypes.new MalList(result);
											continue;
										}
									}

									// case iv - default if none of the above matched
									List<MalType> result = new ArrayList<>();
									result.add(malTypes.new MalSymbol("cons"));

									List<MalType> firstQQ = new ArrayList<>();
									firstQQ.add(malTypes.new MalSymbol("quasiquote"));
									firstQQ.add(astList.nth(0));
									MalType firstQQres = eval(malTypes.new MalList(firstQQ), replEnv);
									result.add(firstQQres);

									List<MalType> secondQQ = new ArrayList<>();
									secondQQ.add(malTypes.new MalSymbol("quasiquote"));
									List<MalType> secondQQarg = new ArrayList<>();
									secondQQarg.addAll(astList.items.subList(1, astList.items.size()));
									MalList secondQQargList = malTypes.new MalList(secondQQarg);
									secondQQ.add(secondQQargList);
									MalType secondQQres = eval(malTypes.new MalList(secondQQ), replEnv);
									result.add(secondQQres);
									ast = malTypes.new MalList(result);
									continue;
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
									replEnv = new env(funcTco.functionEnv, funcTco.params, funcArgs);
									continue;
								}
							}
						} else {
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

	private static boolean is_pair(MalType param) { // is a non-empty list
		return param instanceof MalList && ((MalList)param).items.size() > 0;
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
		} catch (RepException rex) {
			return rex.getMessage();
		} catch (Exception e) {
			return "EOF"; // generic error
		}
	}

	public static void main(String... args) {
		step7_quote rp = new step7_quote();

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
		// rp.rep("(quasiquote ((1)))");

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
