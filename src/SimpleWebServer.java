import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class SimpleWebServer {
    private static final int PORT = 8080;
    private static final String WEB_ROOT = "./webroot"; // Carpeta raíz para archivos estáticos
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(10); // Manejo de concurrencia
    private ServerSocket serverSocket;

    public static void main(String[] args) {
        SimpleWebServer server = new SimpleWebServer();
        server.start();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Servidor iniciado en el puerto " + PORT);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new ClientHandler(clientSocket, WEB_ROOT));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            stop();
        }
    }

    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            threadPool.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private String webRoot;

    public ClientHandler(Socket clientSocket, String webRoot) {
        this.clientSocket = clientSocket;
        this.webRoot = webRoot;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {
            
            String requestLine = in.readLine();
            if (requestLine != null) {
                String[] requestParts = requestLine.split(" ");
                String method = requestParts[0];
                String path = requestParts[1];
                
                if (method.equals("GET")) {
                    handleGetRequest(path, out);
                } else if (method.equals("POST")) {
                    handlePostRequest(in, out);
                } else {
                    send404(out);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleGetRequest(String path, OutputStream out) throws IOException {
        if (path.equals("/")) {
            path = "/index.html"; // Página principal
        } else if (path.equals("/api/services")) {
            // Manejo del endpoint REST
            String responseBody = "{\"services\": [\"Consultoría\", \"Desarrollo de Software\", \"Soporte Técnico\"]}";
            sendResponse(out, "200 OK", "application/json", new ByteArrayInputStream(responseBody.getBytes()));
            return;
        }

        // Sirviendo archivos estáticos
        File file = new File(webRoot, path);
        if (file.exists() && !file.isDirectory()) {
            sendResponse(out, "200 OK", getContentType(file.getName()), new FileInputStream(file));
        } else {
            send404(out);
        }
    }

    private void handlePostRequest(BufferedReader in, OutputStream out) throws IOException {
        // Aquí podrías manejar la lógica de POST para otros servicios REST
        String responseBody = "{\"message\": \"POST request handled\"}";
        sendResponse(out, "200 OK", "application/json", new ByteArrayInputStream(responseBody.getBytes()));
    }

    private void sendResponse(OutputStream out, String status, String contentType, InputStream body) throws IOException {
        PrintWriter writer = new PrintWriter(out, true);
        writer.println("HTTP/1.1 " + status);
        writer.println("Content-Type: " + contentType);
        writer.println();
        
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = body.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        out.flush();
    }

    private void send404(OutputStream out) throws IOException {
        String responseBody = "<html><body><h1>404 Not Found</h1></body></html>";
        sendResponse(out, "404 Not Found", "text/html", new ByteArrayInputStream(responseBody.getBytes()));
    }

    private String getContentType(String fileName) {
        if (fileName.endsWith(".html")) {
            return "text/html";
        } else if (fileName.endsWith(".css")) {
            return "text/css";
        } else if (fileName.endsWith(".js")) {
            return "application/javascript";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".jpg")) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }
}
