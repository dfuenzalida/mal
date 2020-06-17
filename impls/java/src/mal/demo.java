package mal;

public class demo {
    public static void main(String[] args) {
	String input;
	do {
	    input = System.console().readLine("user> ");
	    System.out.println("input: " + input);
	} while (input != null);
    }
}
