package my.pkg;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.concurrent.CompletableFuture;

public class SqsProvider {

	private final SqsAsyncClient sqsClient;

	public static SqsProvider getInstance(){
		return new SqsProvider();
	}
	private SqsProvider(){
		sqsClient= SqsAsyncClient
			.builder()
			.region(Region.EU_WEST_1)
			.build();

	}
	public  void sqsCall() {

		 getQueueUrlByName(sqsClient,"shay-test")
			.thenAccept(queueUrl->{
				SendMessage(sqsClient,queueUrl).whenComplete((sendMessageResponse, throwable) -> {
					if (throwable != null) {
						throwable.printStackTrace();
					} else {
						System.out.println("Message sent. ID: " + sendMessageResponse.messageId());
					}
				});


		});

	}

	public CompletableFuture<SendMessageResponse> SendMessage(SqsAsyncClient sqsClient, String queueUrl){
		SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
			.queueUrl(queueUrl)
			.messageBody("hello")
			.build();

		return sqsClient.sendMessage(sendMessageRequest);

	}

	public CompletableFuture<String> getQueueUrlByName(SqsAsyncClient sqsAsyncClient, String queueName) {
		GetQueueUrlRequest getQueueUrlRequest = GetQueueUrlRequest.builder()
			.queueName(queueName)
			.build();

		CompletableFuture<GetQueueUrlResponse> futureResponse = sqsAsyncClient.getQueueUrl(getQueueUrlRequest);

		return futureResponse.thenApply(GetQueueUrlResponse::queueUrl);
	}
}
