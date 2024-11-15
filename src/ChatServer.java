import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    // список всех активных клиентов
    private static Set<ClientHandler> clientHandlers = new HashSet<>();

    public static void main(String[] args) {
        System.out.println("Сервер запущен...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                // Ожидание подключения клиента
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                // Добавление клиента в список активных клиентов
                clientHandlers.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
        }
    }

    // метод для рассылки сообщения всем клиентам, кроме отправителя
    public static void broadcastMessage(String message, ClientHandler excludeClient) {
        synchronized (clientHandlers) {
            for (ClientHandler client : clientHandlers) {
                if (client != excludeClient && !client.ignoredUsers.contains(excludeClient.getClientName())) {
                    client.out.println(message);
                }
            }
        }
    }

    // метод для отправки приватного сообщения конкретному клиенту
    public static void sendPrivateMessage(String message, String recipientName, ClientHandler sender) {
        synchronized (clientHandlers) {
            boolean found = false;
            for (ClientHandler client : clientHandlers) {
                if (client.getClientName().equals(recipientName)) {
                    client.out.println("[Приватное от " + sender.getClientName() + "]: " + message);
                    sender.out.println("[Приватное для " + recipientName + "]: " + message);
                    found = true;
                    break;
                }
            }
            if (!found) {
                sender.out.println("Пользователь " + recipientName + " не найден.");
            }
        }
    }

    // удаление клиента из списка активных клиентов
    public static void removeClient(ClientHandler clientHandler) {
        clientHandlers.remove(clientHandler);
    }

    // класс для обработки взаимодействия с конкретным клиентом
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName;
        private Set<String> ignoredUsers = new HashSet<>();
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public String getClientName() {
            return clientName;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                clientName = in.readLine();
                if (clientName == null) {
                    return;
                }
                out.println("Чат:");

                System.out.println(clientName + " присоединился к чату.");
                // уведомление других клиентов о подключении нового пользователя
                broadcastMessage(clientName + " присоединился к чату.", this);

                String message;
                while ((message = in.readLine()) != null) {
                    // проверка ввёл ли клиент команду
                    if (message.startsWith("/msg")) {
                        String[] parts = message.split(" ", 3);
                        if (parts.length == 3) {
                            String recipientName = parts[1];
                            String privateMessage = parts[2];
                            sendPrivateMessage(privateMessage, recipientName, this);
                        } else {
                            out.println("Используйте команду: /msg <имя> <сообщение>");
                        }
                    }
                    else if(message.equals("/list")){
                        // отправляем текущему клиенту список активных пользователей
                        out.println("Список активных пользователей:");
                        synchronized (clientHandlers) {
                            for (ClientHandler client : clientHandlers) {
                                out.println("- " + client.clientName);
                            }
                        }
                    }
                    else if (message.equals("/help")){

                        out.println("Доступные команды:\n" +
                                "/list - Показать список активных пользователей\n" +
                                "/msg <имя> <сообщение> - Отправить приватное сообщение\n" +
                                "/ignore <имя> - Заблокировать пользователя\n" +
                                "/unignore <имя> - Разаблокировать пользователя\n");
                    }
                    else if (message.startsWith("/ignore ")) {

                        String userToIgnore = message.substring(8).trim();
                        ignoredUsers.add(userToIgnore);
                        out.println("Вы игнорируете сообщения от " + userToIgnore);
                    }
                    else if (message.startsWith("/unignore ")) {

                        String userToUnignore = message.substring(10).trim();
                        if (ignoredUsers.remove(userToUnignore)) {
                            out.println("Вы больше не игнорируете сообщения от " + userToUnignore);
                        } else {
                            out.println(userToUnignore + " не находится в списке игнорируемых.");
                        }
                    }
                    else {
                        System.out.println(clientName + ": " + message);
                        broadcastMessage(clientName + ": " + message, this);
                    }
                }
            } catch (SocketException e) {

                System.out.println(clientName + " покинул чат.");
                broadcastMessage(clientName + " покинул чат.", this);

            } catch (IOException e) {
                System.err.println("Ошибка соединения с клиентом: " + e.getMessage());
            } finally {
                removeClient(this);
            }
        }
    }
}
