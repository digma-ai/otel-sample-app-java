package petclinic.client;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ErrorTester {

	private static final Logger logger = LoggerFactory.getLogger(ErrorTester.class);

	private static final String BASE_APP_URL = System.getenv("PETSHOP_URL");

	private static final String BASE_APP_SAMPLE_URL = BASE_APP_URL + "/Errors";

	public static void main(String[] args) {
		logger.info("Starting... URL:" + BASE_APP_URL);

		MyClient myClient = new MyClient(BASE_APP_SAMPLE_URL);
		myClient.execute("/error1", false);
		myClient.execute("/error2", false);
		myClient.execute("/error3", false);
		myClient.execute("/error4", false);
		myClient.execute("/error5", false);
		myClient.execute("/error6", false);
		for (int i = 0; i < 10; i++) {
			myClient.execute("/error7", false);
		}
		myClient.execute("/error8", false);

		myClient.execute("/error9", false);

		myClient.execute("/error10", false);
		myClient.execute("/error11", false);

		myClient.execute("/run-async", false);







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
