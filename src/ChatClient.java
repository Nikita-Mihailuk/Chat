import java.io.*;
import java.net.*;
import java.util.Scanner;
public class ChatClient {
    public static void main(String[] args) {
        new ChatClient();
    }
    // ip-адрес сервера и порт, к которому будет подключаться клиент
    private static final String SERVER_ADDRESS = "7.98.48.204";
    private static final int SERVER_PORT = 12345;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Scanner consoleInput = new Scanner(System.in);

    public ChatClient() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            System.out.println("Подключено к чату. Введите ваше имя:");

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);// true означает автосброс, т.е. каждый вызов println отправляет данные на сервер

            new Thread(new ReadMessage()).start();
            new Thread(new WriteMessage()).start();

        } catch (IOException e) {
            System.err.println("Ошибка подключения к серверу: " + e.getMessage());
        }
    }

    // класс для получения сообщений от сервера
    private class ReadMessage implements Runnable {
        @Override
        public void run() {
            String message;
            try {
                while ((message = in.readLine()) != null) {
                    System.out.println(message);
                }
            } catch (IOException e) {
                System.err.println("Ошибка чтения сообщения: " + e.getMessage());
            }
        }
    }

    // класс для отправки сообщений серверу
    private class WriteMessage implements Runnable {
        @Override
        public void run() {
            String message;
            // чтение сообщений с консоли и отправка их серверу
            while ((message = consoleInput.nextLine()) != null) {
                out.println(message);
            }
        }
    }
}