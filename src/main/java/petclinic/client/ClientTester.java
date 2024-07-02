package petclinic.client;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;

public class ClientTester {

	private static final Logger logger = LoggerFactory.getLogger(ClientTester.class);

	private static final String BASE_APP_URL = System.getenv("PETSHOP_URL");

	private static final String BASE_APP_SAMPLE_URL = BASE_APP_URL + "/SampleInsights";

	public static void main(String[] args) {
		logger.info("Starting... URL:" + BASE_APP_URL);

		MyClient myClient = new MyClient(BASE_APP_URL);

		for (int ix = 1; ix <= 5; ix++) {
			myClient.execute("/vets.html");
		}
		for (int ix = 1; ix <= 2; ix++) {
			myClient.execute("/oups", false);
		}
		for (int ix = 1; ix <= 3; ix++) {
			myClient.execute("/appErr1", false);
		}
		for (int ix = 1; ix <= 4; ix++) {
			myClient.execute("/appErr2", false);
		}

		for (int ix = 1; ix <= 2; ix++) {
			myClient.execute("/");
		}

		for (int ix = 1; ix <= 7; ix++) {
			myClient.execute("/owners/find");
		}
		// few of those might return error, since not all ids exists
		for (int ownerId = 1; ownerId <= 20; ownerId++) {
			myClient.execute("/owners/" + ownerId, false);
		}
		for (int ix = 1; ix <= 6; ix++) {
			myClient.execute("/owners?lastName=Davis");
		}
		for (int ix = 1; ix <= 6; ix++) {
			myClient.execute("/owners?lastName=Spring");
		}

		// calling new owner (getting the form) - which has a delay
		for (int ix = 1; ix <= 9; ix++) {
			myClient.execute("/owners/new");
		}

		// using existing owners
		for (int ownerId = 2; ownerId <= 6; ownerId++) {
			// calling edit (getting the form)
			myClient.execute(String.format("owners/%d/edit", ownerId));
			// calling add new pet (getting the form)
			myClient.execute(String.format("owners/%d/pets/new", ownerId));
		}

		generateInsightData();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				logger.info("Shutting down...");
				Thread.sleep(567);
			}
			catch (InterruptedException e) {
				Thread.interrupted();
			}
		}));
	}

	private static void generateInsightData() {
		MyClient myClient = new MyClient(BASE_APP_SAMPLE_URL);

		logger.info("***** START generating traffic *****");

		logger.info("***** Slow traffic *****");
		for (int ix = 1; ix <= 2; ix++) {
			myClient.execute("/SlowEndpoint?extraLatency=2500");
		}

		logger.info("***** Triggering bottlenecks *****");
		for (int ix = 1; ix <= 2; ix++) {
			myClient.execute("/SpanBottleneck");
		}

		logger.info("***** Errors *****");
		for (int ix = 1; ix <= 2; ix++) {
			myClient.execute("/ErrorHotspot", false);
		}

		logger.info("***** Complex queries *****");
		for (int ix = 1; ix <= 2; ix++) {
			myClient.execute("/NPlusOneWithInternalSpan");
			myClient.execute("/NPlusOneWithoutInternalSpan");
		}

		logger.info("***** Async workers *****");
		for (int ix = 1; ix <= 2; ix++) {
			myClient.execute("/GenAsyncSpanVar01");
		}

		logger.info("***** Load and high usage *****");
		for (int ix = 1; ix <= 400; ix++) {
			myClient.execute("/HighUsage");
		}

		for (int ix = 1; ix <= 2; ix++) {
			myClient.execute("/req-map-get");
		}

		logger.info("***** Nested Operations *****");
		for (int ix = 1; ix <= 2; ix++) {
			myClient.execute("/ErrorRecordedOnDeeplyNestedSpan");
		}

		logger.info("***** Complex errors *****");
		for (int ix = 1; ix <= 2; ix++) {
			myClient.execute("/ErrorRecordedOnLocalRootSpan");
		}

		logger.info("***** Errors records  *****");
		for (int ix = 1; ix <= 2; ix++) {
			myClient.execute("/ErrorRecordedOnCurrentSpan");
		}

		logger.info("***** END generateInsightData *****");
	}

	static class MyClient {

		private OkHttpClient client;

		private final String baseUrl;

		private MyClient(String baseUrl) {
			this.baseUrl = baseUrl;

			this.client = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
				final int MAXTRIES = 20;

				final int DELAY_SECONDS = 3;

				@Override
				public Response intercept(Chain chain) throws IOException {
					Request request = chain.request();
					Response response = null;
					int tryCount = 0;
					boolean noException = false;

					while (!noException && tryCount < MAXTRIES) {
						try {
							response = chain.proceed(request);
							noException = true;
						}
						catch (Exception e) {
							try {
								noException = false;
								logger.info("Waiting for service to be up... Attempt:" + tryCount);
								if (response != null) {
									response.close();
								}

								Thread.sleep(DELAY_SECONDS * 1000);
							}
							catch (InterruptedException ex) {
								throw new RuntimeException(ex);
							}

						}
						finally {
							tryCount++;
						}
					}

					// otherwise just pass the original response on
					return response;
				}
			}).build();

		}

		public void execute(String uriPath, boolean throwExceptionWhenCodeIsNot200) {
			Request request = new Request.Builder().url(baseUrl + assureStartsWithSlash(uriPath))

				.build();

			Call call = client.newCall(request);

			try (Response response = call.execute()) {
				if (throwExceptionWhenCodeIsNot200) {
					int code = response.code();
					if (code != 200) {
						throw new RuntimeException(String.format("URI '%s' returned code %d", uriPath, code));
					}
				}
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public void execute(String uriPath) {
			execute(uriPath, true);
		}

		private static String assureStartsWithSlash(String relativeUri) {
			if (relativeUri == null) {
				return "/";
			}
			String trimmedUri = relativeUri.trim();
			if (trimmedUri.startsWith("/")) {
				return relativeUri;
			}
			return '/' + trimmedUri;
		}

	}

}
