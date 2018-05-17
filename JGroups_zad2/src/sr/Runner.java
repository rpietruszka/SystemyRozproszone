package sr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Runner {
    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        DistributedMap dMap = new DistributedMap();

        try {
            dMap.joinChannel();
        } catch (Exception e) {
            e.printStackTrace();
        }
        printHelp();
        while(true) {
            try {
                String cmd = input.readLine().trim();
                String[] tokens = cmd.split(" ");

                switch (tokens[0]) {
                    case "get":
                        String x = dMap.get(tokens[1]);
                        System.out.println(
                            "<"+tokens[1]+", "+(x != null ? x :"-NONE-") +">");
                        break;

                    case "put":
                        if(tokens.length < 3){
                            System.out.println("Not enough args");
                        } else {
                            dMap.put(tokens[1], tokens[2]);
                            System.out.println("Add <" + tokens[1] + ", " + tokens[2] + ">");
                        }
                        break;

                    case "rm":
                        String val = dMap.remove(tokens[1]);
                        if(val == null) {
                            System.out.println("No key "+tokens[1]+"in map");
                        } else {
                            System.out.println("Removed <"+tokens[1]+", "+val+">");
                        }
                        break;

                    case "cont"://contains
                        System.out.println("Key "+ tokens[1] +" exist: "
                                + (dMap.containsKey(tokens[1]) ? "YES" : "NO"));
                        break;

                    case "merge":
                        dMap.mergeWith(tokens[1]);
                        break;

                    case "?":
                        printHelp();
                        break;

                    case "print":
                        System.out.println(dMap);
                        break;

                    case "exit":
                        dMap.shutdownChannel();
                        System.exit(0);
                        break;

                    default:
                        System.out.println("Unknown command, type '?' for help.");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void printHelp(){
        System.out.println(
                "\nHelp:\n" +
                "\tget <key>\n" +
                "\tput <key> <val>\n" +
                "\trm <key>  //remove\n" +
                "\tcont <key> //contains\n" +
                "\tprint //print map content\n" +
                "\t? //help\n"+
                "\texit //Close connection and exit\n"
        );

    }
}
