import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.ConfirmSubscriptionRequest;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.SubscribeRequest;

import org.codehaus.jackson.map.ObjectMapper;

import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

// Example SNS Receiver
public class AmazonSNSReceiver {

    // AWS credentials -- replace with your credentials
    static String ACCESS_KEY = "<Your AWS Access Key>";
    static String SECRET_KEY = "<Your AWS Secret Key>";

    // Shared queue for notifications from HTTP server
    static BlockingQueue<Map<String, String>> messageQueue = new LinkedBlockingQueue<Map<String, String>>();

    // Receiver loop
    public static void main(String... args) throws Exception {

        // Create a client
        AmazonSNSClient service = new AmazonSNSClient(new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY));

        // Create a topic
        CreateTopicRequest createReq = new CreateTopicRequest()
            .withName("MyTopic");
        CreateTopicResult createRes = service.createTopic(createReq);

        // Get an HTTP Port
        int port = args.length == 1 ? Integer.parseInt(args[0]) : 8989;

        // Create and start HTTP server
        Server server = new Server(port);
        server.setHandler(new AmazonSNSHandler());
        server.start();

        // Subscribe to topic
        SubscribeRequest subscribeReq = new SubscribeRequest()
            .withTopicArn(createRes.getTopicArn())
            .withProtocol("http")
            .withEndpoint("http://" + InetAddress.getLocalHost().getHostAddress() + ":" + port);
        service.subscribe(subscribeReq);

        for (;;) {

            // Wait for a message from HTTP server
            Map<String, String> messageMap = messageQueue.take();

            // Look for a subscription confirmation Token
            String token = messageMap.get("Token");
            if (token != null) {

                // Confirm subscription
                ConfirmSubscriptionRequest confirmReq = new ConfirmSubscriptionRequest()
                    .withTopicArn(createRes.getTopicArn())
                    .withToken(token);
                service.confirmSubscription(confirmReq);

                continue;
            }

            // Check for a notification
            String message = messageMap.get("Message");
            if (message != null) {
                System.out.println("Received message: " + message);
            }
        }
    }

    // HTTP handler
    static class AmazonSNSHandler extends AbstractHandler {

        // Handle HTTP request
        public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException {

            // Scan request into a string
            Scanner scanner = new Scanner(request.getInputStream());
            StringBuilder sb = new StringBuilder();
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine());
            }
            
            // Build a message map from the JSON encoded message
            InputStream bytes = new ByteArrayInputStream(sb.toString().getBytes());
            Map<String, String> messageMap = new ObjectMapper().readValue(bytes, Map.class);

            // Enqueue message map for receive loop
            messageQueue.add(messageMap);

            // Set HTTP response
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            ((Request) request).setHandled(true);
        }        
    }
}
