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

public class step6_file {

	static mal.types malTypes = new types();
	static env repl_env = new env(null, null, null);
	
	public step6_file() {
		core.ns.forEach((name, fn) -> repl_env.set(malTypes.new MalSymbol(name), fn));
	}


	public static MalType eval(MalType ast, env replEnv) {
		while (true) {
			if (!(ast instanceof MalList)) {
				return eval_ast(ast, replEnv);
			} else {
				MalList inputList = (MalList) ast;
				if (inputList.items.isEmpty()) {
					return ast;
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
								replEnv = newEnv;
								ast = letBody;
								continue;
							} else if (firstSymbol.name.equals("do")) {
								// Evaluate all the elements of the list using eval_ast and return the final evaluated element
								for (int i = 1; i < inputList.items.size() - 1; i++) {
									MalType item = inputList.items.get(i);
									// TODO eval_ast or just eval? See: (do (/ 1 0) (+ 1 1)) should crash
									eval(item, replEnv);
								}
								ast = inputList.items.get(inputList.items.size() - 1); // unevaluated
								continue;
							} else if (firstSymbol.name.equals("if")) {
								MalType test = eval(inputList.items.get(1), replEnv);
								if (types.MalNil.equals(test) || types.MalFalse.equals(test)) {
									if (inputList.items.size() == 3) {
										return types.MalNil;
									} else {
										ast = inputList.items.get(3);
										continue;
									}
								} else {
									ast = inputList.items.get(2);
									continue;
								}
							} else if (firstSymbol.name.equals("fn*")) {
								final env replEnvCopy = new env(replEnv, null, null); // required because Java wants replEnv to be final
								MalList binds = (MalList) inputList.items.get(1);
								MalType expr = inputList.items.get(2);
								MalFunction fn = malTypes.new MalFunction() {
									MalType apply(MalList args) {
										// Copy the lists binds and args lists around, if one of the 'binds' is '&',
										// then capture every args afterwards as a list
										MalList bindsNew = malTypes.new MalList(new ArrayList<>());
										MalList argsNew = malTypes.new MalList(new ArrayList<>());
										for (int i = 0; i < binds.items.size(); i++) {
											MalSymbol bindsi = (MalSymbol) binds.items.get(i);
											MalType argsi = (i < args.items.size()) ? args.items.get(i) : types.MalNil;
											if (bindsi.name.equals("&")) {
												bindsi = (MalSymbol) binds.items.get(i + 1); // & more
												if (i < args.items.size()) {
													List<MalType> argItems = new ArrayList<>(args.items.subList(i, args.items.size()));
													argsi = malTypes.new MalList(argItems);
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
							} else if (firstSymbol.name.equals("eval")) {
								MalType expr = inputList.items.get(1);
								MalType exprAst = eval(expr, replEnv);
								return eval(exprAst, replEnv);
							}
						}
						
						// regular function application
						MalList evaluated = (MalList) eval_ast(inputList, replEnv);
						MalType funcOrFunctionTco = evaluated.items.get(0);
						List<MalType> argsSubList = new ArrayList<>(evaluated.items.subList(1, evaluated.items.size()));
						MalList funcArgs = malTypes.new MalList(argsSubList);
						if (funcOrFunctionTco instanceof MalFunction) {
							MalFunction func = (MalFunction) funcOrFunctionTco;
							return func.apply(funcArgs);
						} else {
							FunctionTco funcTco = (FunctionTco) funcOrFunctionTco;
							ast = funcTco.ast;
							replEnv = new env(funcTco.functionEnv, funcTco.params, funcArgs);
							continue;
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
			return printer.pr_str(afterEval, true);
		} catch (RepException rex) {
			return rex.getMessage();
		} catch (Exception e) {
			return "EOF"; // generic error
		}
	}

	public static void main(String... args) {
		step6_file rp = new step6_file();

		if (args.length > 0) {
			String fileName = args[0];
			rp.rep(String.format("(load-file \"%s\")", fileName));
		}

		// load the rest of the args as *ARGV*
		List<String> quotedArgs = Collections.emptyList();
		if (args.length > 1) {
			quotedArgs = Arrays.asList(args).subList(1, args.length).stream()
					.map(a -> String.format("\"%s\"", a))
					.collect(Collectors.toList());
		}
		String defArgv = String.format("(def! *ARGV* (list %s))", String.join(" ", quotedArgs));
		rp.rep(defArgv);
		
		// Local Test

		// Functions defined in MAL itself
		rp.rep("(def! not (fn* (a) (if a false true)))"); // (not <expr>)
		rp.rep("(def! load-file (fn* (f) (eval (read-string (str \"(do \" (slurp f) \"\\nnil)\")))))");
		rp.rep("(load-file \"../tests/incA.mal\")");
		rp.rep("(inc4 1)");

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
