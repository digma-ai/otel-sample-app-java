package my.pkg;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class SqsProvider {
	public void sqsCall() {
		SqsClient sqsClient = SqsClient.builder()
			.region(Region.EU_WEST_1)
			.build();

		GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
			.queueName("shay-test")
			.build();

		String queueUrl = sqsClient.getQueueUrl(getQueueRequest).queueUrl();
		SendMessageRequest hello = SendMessageRequest.builder()
			.queueUrl(queueUrl)
			.messageBody("hello")
			.delaySeconds(1)
			.build();

		sqsClient.sendMessage(hello);
	}
}
