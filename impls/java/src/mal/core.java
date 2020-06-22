package mal;

import java.util.HashMap;
import java.util.Map;

import mal.types.MalFunction;
import mal.types.MalInteger;
import mal.types.MalList;
import mal.types.MalType;

public class core {

	static types malTypes = new types();
	public static Map<String, MalFunction> ns = new HashMap<>();
	static {
		ns.put("+", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalInteger arg0 = (MalInteger) args.items.get(0);
				MalInteger arg1 = (MalInteger) args.items.get(1);
				return malTypes.new MalInteger(arg0.value + arg1.value);
			}
		});

		ns.put("-", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalInteger arg0 = (MalInteger) args.items.get(0);
				MalInteger arg1 = (MalInteger) args.items.get(1);
				return malTypes.new MalInteger(arg0.value - arg1.value);
			}
		});

		ns.put("*", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalInteger arg0 = (MalInteger) args.items.get(0);
				MalInteger arg1 = (MalInteger) args.items.get(1);
				return malTypes.new MalInteger(arg0.value * arg1.value);
			}
		});

		ns.put("/", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalInteger arg0 = (MalInteger) args.items.get(0);
				MalInteger arg1 = (MalInteger) args.items.get(1);
				return malTypes.new MalInteger(arg0.value / arg1.value);
			}
		});

		ns.put("prn", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalType arg0 = args.items.get(0);
				System.out.println(printer.pr_str(arg0));
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
				MalType arg0 = args.items.get(0);
				return (arg0 instanceof MalList) ? types.MalTrue : types.MalFalse;
			}
		});

		ns.put("empty?", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalType arg0 = args.items.get(0);
				boolean result = (arg0 instanceof MalList) && ((MalList)arg0).items.size() == 0;
				return result ? types.MalTrue : types.MalFalse;
			}
		});

		ns.put("count", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				if (args.items.size() == 0 || args.items.get(0) == types.MalNil) {
					return malTypes.new MalInteger(0);
				} else {
					MalList arg0 = (MalList) args.items.get(0);
					return malTypes.new MalInteger(arg0.items.size());
				}
			}
		});

		ns.put("=", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalType arg0 = args.items.get(0);
				MalType arg1 = args.items.get(1);
				if (arg0 instanceof MalList && arg1 instanceof MalList) {
					// every element needs to be equal, same length
					MalList list0 = (MalList) arg0;
					MalList list1 = (MalList) arg1;
					boolean res = list0.items.size() == list1.items.size();
					if (res) {
						for (int i = 0; i < list0.items.size(); i++) {
							MalType list0i = list0.items.get(i);
							MalType list1i = list1.items.get(i);
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
				MalInteger arg0 = (MalInteger) args.items.get(0);
				MalInteger arg1 = (MalInteger) args.items.get(1);
				return (arg0.value < arg1.value) ? types.MalTrue : types.MalFalse;
			}
		});

		ns.put("<=", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalInteger arg0 = (MalInteger) args.items.get(0);
				MalInteger arg1 = (MalInteger) args.items.get(1);
				return (arg0.value <= arg1.value) ? types.MalTrue : types.MalFalse;
			}
		});

		ns.put(">", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalInteger arg0 = (MalInteger) args.items.get(0);
				MalInteger arg1 = (MalInteger) args.items.get(1);
				return (arg0.value > arg1.value) ? types.MalTrue : types.MalFalse;
			}
		});

		ns.put(">=", malTypes.new MalFunction() {
			MalType apply(MalList args) {
				MalInteger arg0 = (MalInteger) args.items.get(0);
				MalInteger arg1 = (MalInteger) args.items.get(1);
				return (arg0.value >= arg1.value) ? types.MalTrue : types.MalFalse;
			}
		});

	};
}
