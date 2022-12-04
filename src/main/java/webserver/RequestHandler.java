package webserver;

import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequest;
import util.HttpRequestUtils;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Map;

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

            if( "/user/create".equals(request.getUrl())){
                Map<String, String> params = request.getParams();
                User newUser = User.builder()
                        .userId(params.get("userId"))
                        .name(params.get("name"))
                        .email(params.get("email"))
                        .password(params.get("password"))
                        .build();
                System.out.println(newUser.toString());
            }


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
        separateUrl(request, tokens[1]);

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
        return request;
    }

    private void separateUrl(HttpRequest req, String exp) {
        if (isBlank(exp)) {
            req.setUrl("/index.html");
            return;
        }

        String requestPath = exp;

        if(exp.contains("?")){
            int idx = exp.indexOf('?');
            requestPath = exp.substring(0, idx);
            req.setParams(HttpRequestUtils.parseQueryString(exp.substring(idx + 1)));
        }

        req.setUrl(isBlank(requestPath) ? "/index.html" : requestPath);
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

