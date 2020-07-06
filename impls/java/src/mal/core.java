package mal;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import mal.types.FunctionTco;
import mal.types.MalAtom;
import mal.types.MalFunction;
import mal.types.MalInteger;
import mal.types.MalList;
import mal.types.MalString;
import mal.types.MalType;

public class core {

	static types malTypes = new types();
	public static Map<String, MalFunction> ns = new HashMap<>();
	static {
		ns.put("+", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalInteger arg0 = (MalInteger) args.nth(0);
				MalInteger arg1 = (MalInteger) args.nth(1);
				return malTypes.new MalInteger(arg0.value + arg1.value);
			}
		});

		ns.put("-", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalInteger arg0 = (MalInteger) args.nth(0);
				MalInteger arg1 = (MalInteger) args.nth(1);
				return malTypes.new MalInteger(arg0.value - arg1.value);
			}
		});

		ns.put("*", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalInteger arg0 = (MalInteger) args.nth(0);
				MalInteger arg1 = (MalInteger) args.nth(1);
				return malTypes.new MalInteger(arg0.value * arg1.value);
			}
		});

		ns.put("/", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalInteger arg0 = (MalInteger) args.nth(0);
				MalInteger arg1 = (MalInteger) args.nth(1);
				return malTypes.new MalInteger(arg0.value / arg1.value);
			}
		});

		ns.put("pr-str", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				List<String> strItems = args.items.stream().map(i -> i.toString(true)).collect(Collectors.toList());
				String joined = String.join(" ", strItems);
				return malTypes.new MalString(joined);
			}
		});

		ns.put("str", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				List<String> strItems = args.items.stream().map(i -> i.toString(false)).collect(Collectors.toList());
				String joined = String.join("", strItems);
				return malTypes.new MalString(joined);
			}
		});

		ns.put("prn", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				List<String> strItems = args.items.stream().map(i -> i.toString(true)).collect(Collectors.toList());
				String joined = String.join(" ", strItems);
				System.out.println(joined);
				return types.MalNil;
			}
		});

		ns.put("println", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				List<String> strItems = args.items.stream().map(i -> i.toString(false)).collect(Collectors.toList());
				String joined = String.join(" ", strItems);
				System.out.println(joined);
				return types.MalNil;
			}
		});

		ns.put("list", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				return args;
			}
		});

		ns.put("list?", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalType arg0 = args.nth(0);
				return (arg0 instanceof MalList && ((MalList)arg0).open.equals("(")) ? types.MalTrue : types.MalFalse;
			}
		});

		ns.put("empty?", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalType arg0 = args.nth(0);
				boolean result = (arg0 instanceof MalList) && ((MalList)arg0).items.size() == 0;
				return result ? types.MalTrue : types.MalFalse;
			}
		});

		ns.put("count", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				if (args.items.size() == 0 || args.nth(0) == types.MalNil) {
					return malTypes.new MalInteger(0);
				} else {
					MalList arg0 = (MalList) args.nth(0);
					return malTypes.new MalInteger(arg0.items.size());
				}
			}
		});

		ns.put("=", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalType arg0 = args.nth(0);
				MalType arg1 = args.nth(1);
				if (arg0 instanceof MalList && arg1 instanceof MalList) {
					// every element needs to be equal, same length
					MalList list0 = (MalList) arg0;
					MalList list1 = (MalList) arg1;
					boolean res = list0.items.size() == list1.items.size();
					if (res) {
						for (int i = 0; i < list0.items.size(); i++) {
							MalType list0i = list0.nth(i);
							MalType list1i = list1.nth(i);
							if (!list0i.equals(list1i)) {
								res = false;
								break;
							}
						}
					}
					return res ? types.MalTrue : types.MalFalse;
				} else {
					// same class and same value
					return arg0.equals(arg1) ? types.MalTrue : types.MalFalse;
				}
			}
		});

		ns.put("<", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalInteger arg0 = (MalInteger) args.nth(0);
				MalInteger arg1 = (MalInteger) args.nth(1);
				return (arg0.value < arg1.value) ? types.MalTrue : types.MalFalse;
			}
		});

		ns.put("<=", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalInteger arg0 = (MalInteger) args.nth(0);
				MalInteger arg1 = (MalInteger) args.nth(1);
				return (arg0.value <= arg1.value) ? types.MalTrue : types.MalFalse;
			}
		});

		ns.put(">", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalInteger arg0 = (MalInteger) args.nth(0);
				MalInteger arg1 = (MalInteger) args.nth(1);
				return (arg0.value > arg1.value) ? types.MalTrue : types.MalFalse;
			}
		});

		ns.put(">=", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalInteger arg0 = (MalInteger) args.nth(0);
				MalInteger arg1 = (MalInteger) args.nth(1);
				return (arg0.value >= arg1.value) ? types.MalTrue : types.MalFalse;
			}
		});

		ns.put("read-string", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalString arg0 = (MalString) args.nth(0);
				return reader.read_str(arg0.value);
			}
		});

		ns.put("slurp", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				try {
					MalString arg0 = (MalString) args.nth(0);
					String fileName = arg0.value;
					String contents = String.join("\n", Files.readAllLines(Paths.get(fileName))) + "\n";
					return malTypes.new MalString(contents);
				} catch (Exception ex) {
					throw malTypes.new RepException(ex.getMessage());
				}
			}
		});

		ns.put("atom", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalType arg0 = args.nth(0);
				return malTypes.new MalAtom(arg0);
			}
		});

		ns.put("atom?", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalType arg0 = args.nth(0);
				return (arg0 instanceof MalAtom) ? types.MalTrue : types.MalFalse;
			}
		});

		ns.put("deref", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalAtom arg0 = (MalAtom) args.nth(0);
				return arg0.value;
			}
		});

		ns.put("reset!", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalAtom arg0 = (MalAtom) args.nth(0);
				MalType arg1 = args.nth(1);
				return arg0.value = arg1;
			}
		});

		ns.put("swap!", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalAtom atom = (MalAtom) args.nth(0);
				MalType fnOrFunctionTco = args.nth(1);
				List<MalType> fnArgs = new ArrayList<>();
				fnArgs.add(atom.value);
				if (args.items.size() > 2) { fnArgs.addAll(args.items.subList(2, args.items.size())); }
				MalList fnArgsMalList = malTypes.new MalList(fnArgs);
				MalType retVal;

				if (fnOrFunctionTco instanceof MalFunction) {
					MalFunction fn = (MalFunction) fnOrFunctionTco;
					retVal = fn.apply(fnArgsMalList);
				} else {
					FunctionTco funcTco = (FunctionTco) fnOrFunctionTco;
					MalType ast = funcTco.ast;
					env replEnv = new env(funcTco.functionEnv, funcTco.params, fnArgsMalList);
					retVal = step6_file.eval(ast, replEnv);
				}
				atom.value = retVal;
				return retVal;
			}
		});

		ns.put("cons", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalType arg0 = args.nth(0);
				MalList arg1 = (MalList) args.nth(1);
				List<MalType> consList = new ArrayList<>();
				consList.add(arg0);
				consList.addAll(arg1.items);
				return malTypes.new MalList(consList);
			}
		});

		ns.put("concat", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				List<MalType> concatList = new ArrayList<>();
				for (MalType list: args.items) {
					concatList.addAll(((MalList) list).items); 
				}
				return malTypes.new MalList(concatList);
			}
		});

		ns.put("nth", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				try {
					MalList arg0 = (MalList) args.nth(0);
					MalInteger arg1 = (MalInteger) args.nth(1);
					return arg0.nth(arg1.value);
				} catch (Exception ex) {
					throw malTypes.new RepException("Cannot call nth on " + args.nth(0));
				}
			}
		});

		ns.put("first", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				if (args.nth(0) == types.MalNil) return types.MalNil;
				MalList arg0 = (MalList) args.nth(0);
				if (arg0.items.isEmpty()) return types.MalNil;
				return arg0.nth(0);
			}
		});

		ns.put("rest", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				if (args.nth(0) == types.MalNil) {
					return malTypes.new MalList(Collections.emptyList());
				}
				MalList arg0 = (MalList) args.nth(0);
				if (arg0.items.isEmpty()) {
					return malTypes.new MalList(Collections.emptyList());
				}
				List<MalType> rest = new ArrayList<>();
				rest.addAll(arg0.items.subList(1, arg0.items.size()));
				return malTypes.new MalList(rest);
			}
		});

	};
}
