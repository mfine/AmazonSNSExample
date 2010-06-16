import java.util.Date;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.PublishRequest;

// Example SNS Sender
public class AmazonSNSSender {

    // AWS credentials -- replace with your credentials
    static String ACCESS_KEY = "<Your AWS Access Key>";
    static String SECRET_KEY = "<Your AWS Secret Key>";

    // Sender loop
    public static void main(String... args) throws Exception {

        // Create a client
        AmazonSNSClient service = new AmazonSNSClient(new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY));

        // Create a topic
        CreateTopicRequest createReq = new CreateTopicRequest()
            .withName("MyTopic");
        CreateTopicResult createRes = service.createTopic(createReq);

        for (;;) {

            // Publish to a topic
            PublishRequest publishReq = new PublishRequest()
                .withTopicArn(createRes.getTopicArn())
                .withMessage("Example notification sent at " + new Date());
            service.publish(publishReq);

            Thread.sleep(1000);
        }
    }
}
