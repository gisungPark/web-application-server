package webserver;

import com.google.common.base.Strings;
import db.DataBase;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;
    private final String DEFAULT_URL = "/index.html";

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {

            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line = br.readLine();

            if(line == null) return;

            String[] tokens = line.split(" ");
            int contentLength = 0;
            String cookies = "";

            while (!"".equals(line)) {
                if (line.contains("Content-Length")) {
                    contentLength = getContentLength(line);
                }
                if (line.contains("Cookie")) {
                    System.out.println(line);
                    cookies = line;
                }
                line = br.readLine();
            }

            String url = getDefaultUrl(tokens);

            if ("/user/create".equals(url)) {
                String body = IOUtils.readData(br, contentLength);
                Map<String, String> params = HttpRequestUtils.parseQueryString(body);
                User user = new User(params.get("userId"), params.get("password"), params.get("name"),
                        params.get("email"));
                DataBase.addUser(user);
                log.debug("user : {}", user);

                DataOutputStream dos = new DataOutputStream(out);
                response302Header(dos);
            } else if ("/user/login".equals(url)) {
                String body = IOUtils.readData(br, contentLength);
                Map<String, String> params = HttpRequestUtils.parseQueryString(body);

                User findUser = DataBase.findUserById(params.get("userId"));

                if (findUser != null && findUser.login(params.get("password"))) {
                        loginSuccessResource(out);
                }
                responseResource(out, "/user/login_failed.html");

            } else if ("/user/list".equals(url)) {
                if (!isLogin(cookies)) {
                    responseResource(out, "/user/login.html");
                    return;
                }

                Collection<User> users = DataBase.findAll();

                StringBuilder sb = new StringBuilder();
                sb.append("<table>");
                for (User user : users) {
                    sb.append("<tr>");
                    sb.append("<td>" + user.getUserId() + "</td>");
                    sb.append("<td>" + user.getName() + "</td>");
                    sb.append("<td>" + user.getEmail() + "</td>");
                    sb.append("</tr>");
                }
                sb.append("</table>");

                byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
                DataOutputStream dos = new DataOutputStream(out);
                response200Header(dos, body.length);
                responseBody(dos, body);

            } else {
                responseResource(out, "/user/login.html");
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseResource(OutputStream out, String url) throws IOException {
        byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
        DataOutputStream dos = new DataOutputStream(out);
        response200Header(dos, body.length);
        responseBody(dos, body);
    }

    private boolean isLogin(String cookies) {
        Map<String, String> stringMap = HttpRequestUtils.parseCookies(cookies);
        String logined = stringMap.get("logined");

        if(Strings.isNullOrEmpty(logined) || "false".equals(logined)) return false;
        return true;
    }

    private void loginFailResource(OutputStream out) {
        try {
            DataOutputStream dos = new DataOutputStream(out);
            dos.writeBytes("HTTP/1.1 401 Unauthorized \r\n");
            dos.writeBytes("Content-Type: text/html \r\n");
            dos.writeBytes("Set-Cookie: logined=false \r\n");

            byte[] body = Files.readAllBytes(new File("./webapp" + "/user/login_failed.html").toPath());
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void loginSuccessResource(OutputStream out) {
        try {
            DataOutputStream dos = new DataOutputStream(out);
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html \r\n");
            dos.writeBytes("Set-Cookie: logined=true \r\n");
            dos.writeBytes("Location: /index.html \r\n");
            dos.writeBytes("\r\n");

            byte[] body = Files.readAllBytes(new File("./webapp" + DEFAULT_URL).toPath());
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private String getDefaultUrl(String[] tokens) {
        if(tokens.length < 1 || "/".equals(tokens[1])) return "/index.html";
        return tokens[1];
    }

    private int getContentLength(String line) {
        String[] split = line.split(":");
        return Integer.valueOf(split[1].trim());
    }

    private void response302Header(DataOutputStream dos) {
        try {
            dos.writeBytes("HTTP/1.1 302 Redirect \r\n");
            dos.writeBytes("Location: /index.html \r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
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

