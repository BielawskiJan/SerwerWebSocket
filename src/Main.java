package src;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.Base64;

public class Main {
    private static final int PORT = 8080;
    private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Listening on port " + PORT);

        while (true) {
            Socket client = serverSocket.accept();
            System.out.println("Client connected from " + client.getInetAddress());

            handleClient(client);
        }
    }

    private static void handleClient(Socket client) {
        try {
            InputStream input = client.getInputStream();
            OutputStream output = client.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            // Odczytaj HTTP handshake
            String line;
            String websocketKey = null;
            while (!(line = reader.readLine()).isEmpty()) {
                if (line.startsWith("Sec-WebSocket-Key:")) {
                    websocketKey = line.substring("Sec-WebSocket-Key:".length()).trim();
                }
            }

            if (websocketKey == null) {
                System.out.println("No WebSocket key, closing.");
                client.close();
                return;
            }

            // Oblicz odpowiedź (Sec-WebSocket-Accept)
            String acceptKey = generateAcceptKey(websocketKey);

            // Wyślij odpowiedź handshake
            String response = "HTTP/1.1 101 Switching Protocols\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n";

            output.write(response.getBytes());
            output.flush();

            System.out.println("Handshake complete with client.");
            // TODO: przejść do obsługi ramek w Etapie 3

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String generateAcceptKey(String websocketKey) {
        try {
            String combined = websocketKey + GUID;
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] sha1 = md.digest(combined.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(sha1);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate accept key", e);
        }
    }
}
