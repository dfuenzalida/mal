package mal;

public class step0_repl {

    public String read(String input) {
	return input;
    }
  
    public String eval(String input) {
	return input;
    }
  
    public String print(String input) {
	return input;
    }
  
    public String rep(String input) {
	String afterRead = this.read(input);
	String afterEval = this.eval(afterRead);
	String afterPrint = this.print(afterEval);
	return afterPrint;
    }
  
    public static void main(String... args) {
	String input;
	do {
	    input = System.console().readLine("user> ");
	    if (input != null) {
		System.out.println(input);
	    }
	} while (input != null);
    }
}
