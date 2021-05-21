import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final ExecutorService executorService;

    private final Map<String, Map<String, Handler>> handlers;

    private final Handler notFoundHandler = (request, out) -> {
        try {
            out.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    };

    public Server(int threadPoolsize) {
        executorService = Executors.newFixedThreadPool(threadPoolsize);
        handlers = new ConcurrentHashMap<>();
        // 2
    }

    public void handleConnection(Socket socket) {
        try (
                socket;
                final var in = socket.getInputStream();
                final var out = new BufferedOutputStream(socket.getOutputStream());
        ) {
            var request = Request.fromInputStream(in);

            Optional.ofNullable(handlers.get(request.getMethod()))
                    .map(pathToHandlerMap -> pathToHandlerMap.get(request.getPath()))
                    .ifPresentOrElse(handler -> handler.handle(request, out),
                            () -> notFoundHandler.handle(request, out));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void fromInputStream(InputStream inputStream) throws IOException {
        final var in = new BufferedReader(new InputStreamReader(inputStream));
        // read only request line for simplicity
        // must be in form GET /path HTTP/1.1
        final var requestLine = in.readLine();
        final var parts = requestLine.split(" ");

        if (parts.length != 3) {
            // just close socket
            throw new IOException("Invalid request");
        }
    }

}