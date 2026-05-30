public class lb {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: java lb <file.lb>");
            return;
        }
        new Interpreter().runFile(args[0]);
    }
}