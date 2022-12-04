package webserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequest;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.

            HttpRequest request = readReqInfo(in);

            byte[] body = Files.readAllBytes(new File("./webapp" + request.getUrl()).toPath());
            DataOutputStream dos = new DataOutputStream(out);
            response200Header(dos, body.length);
            responseBody(dos, body);

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private HttpRequest readReqInfo(InputStream in) throws IOException {
        HttpRequest request = new HttpRequest();
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line = br.readLine();

        if (line == null) {
            request.setUrl("/index.html");
            return request;
        }

        String[] tokens = line.split(" ");
        request.setUrl(isBlank(tokens[1]) ? "/index.html" : tokens[1]);

        while (!"".equals(line)) {
            if (line.contains("Content-Length")) {
                request.getHeader().put("Content-Length", line);
                log.debug("Content-Length : {}", line);
            }

            if (line.contains("Cookie")) {
                request.getHeader().put("Cookie", line);
                log.debug("Cookie : {}", line);
            }
            line = br.readLine();
        }

        log.debug(request.toString());
        return request;
    }

    private boolean isBlank(String url) {
        return url == null || url.isEmpty() || "/".equals(url);
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}

