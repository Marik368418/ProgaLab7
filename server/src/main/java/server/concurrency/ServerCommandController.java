package server.concurrency;

import java.util.Scanner;

public class ServerCommandController implements Runnable{
    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String command = scanner.nextLine();
            switch (command) {
                case "exit" -> {
                    System.out.println("На выход!");
                    System.exit(0);
                }
                default -> System.out.println("Неизвестная команда: " + command);
            }
        }
    }
}
