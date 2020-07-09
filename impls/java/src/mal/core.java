package mal;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import mal.types.FunctionTco;
import mal.types.MalAtom;
import mal.types.MalFunction;
import mal.types.MalHashMap;
import mal.types.MalInteger;
import mal.types.MalKeyword;
import mal.types.MalList;
import mal.types.MalString;
import mal.types.MalSymbol;
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
					String err = "Invalid range when calling nth: " + args.nth(0);
					throw malTypes.new MalException(malTypes.new MalString(err));
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

		ns.put("throw", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalType arg0 = args.nth(0);
				throw malTypes.new MalException(arg0);
			}
		});

		ns.put("apply", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalType fnOrFunctionTco = args.nth(0);
				List<MalType> fnArgs = new ArrayList<>();
				for (Integer i = 1; i < args.items.size() - 1; i++) {
					fnArgs.add(args.nth(i));
				}
				MalType lastArg = args.items.get(args.items.size() - 1);
				if (lastArg instanceof MalList) {
					fnArgs.addAll(((MalList) lastArg).items);
				} else {
					fnArgs.add(lastArg);
				}

				MalFunction fn;
				if (fnOrFunctionTco instanceof MalFunction) {
					fn = (MalFunction) fnOrFunctionTco;
				} else {
					fn = ((FunctionTco) fnOrFunctionTco).fn;
				}
				return fn.apply(malTypes.new MalList(fnArgs));
			}
		});

		ns.put("map", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalType fnOrFunctionTco = args.nth(0);
				MalList coll = (MalList) args.nth(1);
				MalFunction fn;
				if (fnOrFunctionTco instanceof MalFunction) {
					fn = (MalFunction) fnOrFunctionTco;
				} else {
					fn = ((FunctionTco) fnOrFunctionTco).fn;
				}

				List<MalType> vals = new ArrayList<>();
				for (MalType val: coll.items) {
					MalList fnArgs = malTypes.new MalList(Arrays.asList(val));
					vals.add(fn.apply(fnArgs));
				}

				return malTypes.new MalList(vals);
			}
		});

		ns.put("nil?", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalType arg0 = args.nth(0);
				return (arg0 == types.MalNil) ? types.MalTrue : types.MalFalse;
			}
		});

		ns.put("true?", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalType arg0 = args.nth(0);
				return (arg0 == types.MalTrue) ? types.MalTrue : types.MalFalse;
			}
		});

		ns.put("false?", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalType arg0 = args.nth(0);
				boolean isFalse = (arg0 == types.MalNil || arg0 == types.MalFalse);
				return isFalse ? types.MalTrue : types.MalFalse;
			}
		});

		ns.put("symbol?", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalType arg0 = args.nth(0);
				return (arg0 instanceof MalSymbol)? types.MalTrue : types.MalFalse;
			}
		});

		ns.put("symbol", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalString arg0 = (MalString) args.nth(0);
				return malTypes.new MalSymbol(arg0.value);
			}
		});

		ns.put("keyword?", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalType arg0 = args.nth(0);
				return (arg0 instanceof MalKeyword)? types.MalTrue : types.MalFalse;
			}
		});

		ns.put("keyword", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalType arg0 = args.nth(0);
				if (arg0 instanceof MalKeyword) return arg0;
				return malTypes.new MalKeyword(((MalString)arg0).value);
			}
		});

		ns.put("vector", malTypes.new MalFunction() { // poor man's vector
			MalType apply(MalList args) {
				args.open = "[";
				args.close = "]";
				return args;
			}
		});

		ns.put("vector?", malTypes.new MalFunction() { // poor man's vector
			MalType apply(MalList args) {
				MalType arg0 = args.nth(0);
				boolean isVector = (arg0 instanceof MalList && ((MalList)arg0).open.equals("[") && ((MalList)arg0).close.equals("]"));
				return isVector ? types.MalTrue : types.MalFalse;
			}
		});

		ns.put("sequential?", malTypes.new MalFunction() { // poor man's vector
			MalType apply(MalList args) {
				MalType arg0 = args.nth(0);
				boolean isSequential = false;
				if (arg0 instanceof MalList) {
					MalList arg0List = (MalList) arg0;
					isSequential = ("[(".contains(arg0List.open) && "])".contains(arg0List.close));
				}
				return isSequential ? types.MalTrue : types.MalFalse;
			}
		});

		ns.put("hash-map", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalHashMap result = malTypes.new MalHashMap(Collections.emptyMap());
				for (Integer i = 0; i < args.items.size() - 1; i += 2) {
					MalType key = args.nth(i);
					MalType val = args.nth(i + 1);
					result.pairs.put(key, val);
				}
				return result;
			}
		});

		ns.put("map?", malTypes.new MalFunction() { // poor man's vector
			MalType apply(MalList args) {
				MalType arg0 = args.nth(0);
				return (arg0 instanceof MalHashMap) ? types.MalTrue : types.MalFalse;
			}
		});

		ns.put("assoc", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalHashMap source = (MalHashMap) args.nth(0);
				MalHashMap result = malTypes.new MalHashMap(source.pairs);
				for (Integer i = 1; i < args.items.size() - 1; i += 2) {
					MalType key = args.nth(i);
					MalType val = args.nth(i + 1);
					result.pairs.put(key, val);
				}
				return result;
			}
		});

		ns.put("dissoc", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalHashMap source = (MalHashMap) args.nth(0);
				MalHashMap result = malTypes.new MalHashMap(source.pairs);
				for (Integer i = 1; i < args.items.size(); i++) {
					MalType key = args.nth(i);
					result.pairs.remove(key);
				}
				return result;
			}
		});

		ns.put("get", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				if (!(args.nth(0) instanceof MalHashMap)) return types.MalNil;
				MalHashMap source = (MalHashMap) args.nth(0);
				MalType key = args.nth(1);
				return source.pairs.getOrDefault(key, types.MalNil);
			}
		});

		ns.put("contains?", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalHashMap arg0 = (MalHashMap) args.nth(0);
				MalType key = args.nth(1);
				return (arg0.pairs.containsKey(key)) ? types.MalTrue : types.MalFalse;
			}
		});

		ns.put("keys", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalHashMap arg0 = (MalHashMap) args.nth(0);
				return malTypes.new MalList(arg0.pairs.keySet());
			}
		});

		ns.put("vals", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalHashMap arg0 = (MalHashMap) args.nth(0);
				return malTypes.new MalList(arg0.pairs.values());
			}
		});

	};
}
