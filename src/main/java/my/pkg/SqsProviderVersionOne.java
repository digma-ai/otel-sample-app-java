package my.pkg;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class SqsProviderVersionOne {

	private final AmazonSQS sqs;

	public static SqsProviderVersionOne getInstance(){
		return new SqsProviderVersionOne();
	}

	private SqsProviderVersionOne(){
		sqs = AmazonSQSClientBuilder.standard()
			.withRegion("eu-west-1")
			.build();
	}

	public void sqsCall() {

		String message = "Hello, World!";

		String queueUrl = sqs.getQueueUrl("shay-test").getQueueUrl();

		SendMessageRequest sendMessageRequest = new SendMessageRequest(queueUrl, message);
		sqs.sendMessage(sendMessageRequest);

		System.out.println("Message sent to the queue: " + message);
	}

}
