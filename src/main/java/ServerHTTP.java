import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import sun.tools.jconsole.inspector.Utils;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;

// https://www.codeproject.com/Tips/1040097/Create-simple-http-server-in-Java
// run as client with : curl -v -H "Accept: application/json" http://0.0.0.0:9000/
public class ServerHTTP {
    private static Date d = new Date();
    private static SimpleDateFormat ft =
            new SimpleDateFormat ("HH:mm:ss");

    public static void main(String[] args) {
        try {
            int port = 9000;
            InetSocketAddress host = new InetSocketAddress(port);
            HttpServer server = HttpServer.create(host, 0);
            System.out.println("server started at " + host.getHostName() + ":" +port);
            server.createContext("/", new TimeHandler());
            server.setExecutor(null);
            server.start();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static class TimeHandler implements HttpHandler {

        public void handle(HttpExchange he) throws IOException {
            String requestMethod = he.getRequestMethod();
            System.out.println("Received" + requestMethod);
            // GET REQUEST
            if (requestMethod.equalsIgnoreCase("GET")) {
                // Client should send only one accept type
                List<String> acceptTypes = he.getRequestHeaders().get("Accept");

                // Time
                SimpleDateFormat localDateFormat = new SimpleDateFormat("HH:mm:ss");
                String time = localDateFormat.format(d);
                System.out.println(time);

                String payload = "";
                String acceptType = acceptTypes.get(0);
                Headers responseHeaders = he.getResponseHeaders();
                String contentType = "";
                if (acceptType.contains("json")) {
                    contentType = "application/json";
                    payload = "{ \"date\" : \"" + time + "\" }";
                } else if (acceptType.contains("xml")) {
                    contentType = "application/xml";
                    payload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<root>\n" +
                            "   <date>" + time + "</date>\n" +
                            "</root>";
                } else if (acceptType.contains("html")) {
                    contentType = "text/html";
                    payload = "<!doctype html>\n" +
                            "\n" +
                            "<html lang=\"en\">\n" +
                            "<head>\n" +
                            "  <meta charset=\"utf-8\">\n" +
                            "\n" +
                            "</head>\n" +
                            "\n" +
                            "<body>\n" +
                            "  <h1>" + time  + "</h1>" +
                            "</body>\n" +
                            "</html>";
                } else {
                    // By default send text/html
                    contentType = "text/html";
                    payload = "<!doctype html>\n" +
                            "\n" +
                            "<html lang=\"en\">\n" +
                            "<head>\n" +
                            "  <meta charset=\"utf-8\">\n" +
                            "\n" +
                            "</head>\n" +
                            "\n" +
                            "<body>\n" +
                            "  <h1>" + time + "</h1>" +
                            "</body>\n" +
                            "</html>";
                }
                System.out.println(he.getRequestHeaders().values());
                he.getResponseHeaders().add("Content-type", contentType);
                he.sendResponseHeaders(200, payload.length());
                OutputStream os = he.getResponseBody();
                os.write(payload.getBytes());
                os.close();

                // POST REQUEST
            } else if (requestMethod.equalsIgnoreCase("POST")) {
                // Update date
                Map<String, Object> parameters = new HashMap<String, Object>();
                InputStreamReader isr = new InputStreamReader(he.getRequestBody(), "utf-8");
                BufferedReader br = new BufferedReader(isr);
                String query = br.readLine();
                parseQuery(query, parameters);

                // send response
                String response = "";
                String rcvDate = "";
                for (String key : parameters.keySet())
                    rcvDate = parameters.get(key).toString();

                try {
                    d = ft.parse(rcvDate);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    // Send error
                    he.sendResponseHeaders(400, response.length());
                    OutputStream os = he.getResponseBody();
                    os.write(response.toString().getBytes());
                    os.write("Error of type of time. Should be HH:mm:ss".getBytes());
                    os.close();
                }
                System.out.println("Received time update : " + rcvDate);
                he.sendResponseHeaders(200, response.length());
                OutputStream os = he.getResponseBody();
                os.write(response.toString().getBytes());
                os.close();
            } else {
                // send response
                String response = "Not implemented";
                he.sendResponseHeaders(200, response.length());
                he.getRequestHeaders().set("Access-Control-Request-Methods", "GET, POST");

                OutputStream os = he.getResponseBody();
                os.write(response.toString().getBytes());
                os.close();
            }

        }
    }

    public static void parseQuery(String query, Map<String,
            Object> parameters) throws UnsupportedEncodingException {

        if (query != null) {
            String pairs[] = query.split("[&]");
            for (String pair : pairs) {
                String param[] = pair.split("[=]");
                String key = null;
                String value = null;
                if (param.length > 0) {
                    key = URLDecoder.decode(param[0],
                            System.getProperty("file.encoding"));
                }

                if (param.length > 1) {
                    value = URLDecoder.decode(param[1],
                            System.getProperty("file.encoding"));
                }

                if (parameters.containsKey(key)) {
                    Object obj = parameters.get(key);
                    if (obj instanceof List<?>) {
                        List<String> values = (List<String>) obj;
                        values.add(value);

                    } else if (obj instanceof String) {
                        List<String> values = new ArrayList<String>();
                        values.add((String) obj);
                        values.add(value);
                        parameters.put(key, values);
                    }
                } else {
                    parameters.put(key, value);
                }
            }
        }
    }


}
