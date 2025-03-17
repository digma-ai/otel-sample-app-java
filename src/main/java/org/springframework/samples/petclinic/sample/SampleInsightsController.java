package org.springframework.samples.petclinic.sample;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.petclinic.system.AppException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.*;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/SampleInsights")
public class SampleInsightsController implements InitializingBean {

	@Autowired
	private OpenTelemetry openTelemetry;

	private Tracer otelTracer;

	private ExecutorService executorService;

	private RestTemplate restTemplate = new RestTemplate();

	@Override
	public void afterPropertiesSet() throws Exception {
		this.otelTracer = openTelemetry.getTracer("SampleInsightsController");
		this.executorService = Executors.newFixedThreadPool(200);
	}

	@GetMapping("/SpanBottleneck")
	public String genSpanBottleneck() {
		doWorkForBottleneck1();
		doWorkForBottleneck2();
		return "SpanBottleneck";
	}

	@WithSpan(value = "SpanBottleneck 1")
	private void doWorkForBottleneck1() {
		delay(200);
	}

	@WithSpan(value = "SpanBottleneck 2")
	private void doWorkForBottleneck2() {
		delay(50);
	}

	@GetMapping("/SlowEndpoint")
	public String genSlowEndpoint(@RequestParam(name = "extraLatency") long extraLatency) {
		delay(extraLatency);
		return "SlowEndpoint";
	}

	@GetMapping("/HighUsage")
	public String genHighUsage() {
		delay(5);
		return "highUsage";
	}

	// it throws RuntimeException - which supposed to raise error hotspot
	@GetMapping("ErrorHotspot")
	public String genErrorHotspot() {
		method1();
		return "ErrorHotspot";
	}

	private void method1() {
		method2();
	}

	private void method2() {
		method3();
	}

	@WithSpan
	private void method3() {
		throw new RuntimeException("Some unexpected runtime exception");
	}

	@GetMapping("ErrorRecordedOnDeeplyNestedSpan")
	public String genErrorRecordedOnDeeplyNestedSpan() {
		methodThatRecordsError();
		return "ErrorRecordedOnDeeplyNestedSpan";
	}

	private void methodThatRecordsError() {
		Span span = otelTracer.spanBuilder("going-to-record-error").startSpan();

		try {
			throw new AppException("some message");
		}
		catch (AppException e) {
			span.recordException(e);
			span.setStatus(StatusCode.ERROR);
		}
		finally {
			span.end();
		}
	}

	@GetMapping("ErrorRecordedOnCurrentSpan")
	public String genErrorRecordedOnCurrentSpan() {
		Span span = Span.current();
		try {
			throw new AppException("on current span");
		}
		catch (AppException e) {
			span.recordException(e);
			span.setStatus(StatusCode.ERROR);
		}
		return "ErrorRecordedOnCurrentSpan";
	}

	@GetMapping("ErrorRecordedOnLocalRootSpan")
	public String genErrorRecordedOnLocalRootSpan() {
		Span span = LocalRootSpan.current();
		try {
			throw new AppException("on local root span");
		}
		catch (AppException e) {
			span.recordException(e);
			span.setStatus(StatusCode.ERROR);
		}
		return "ErrorRecordedOnLocalRootSpan";
	}

	// using RequestMapping as sanity check for digma version 0.5.32
	@RequestMapping(value = "req-map-get", method = GET)
	public String reqMapOfGet() {
		return "Welcome";
	}

	@GetMapping("GenAsyncSpanVar01")
	public String genAsyncSpanVar01() {
		executorService.submit(() -> {
			doSomeWorkA(654);
		});

		doSomeWorkB(5);

		return "genAsyncSpanVar01";
	}

	@GetMapping("NPlusOneWithoutInternalSpan")
	public String genNPlusOneWithoutInternalSpan() {
		for (int i = 0; i < 100; i++) {
			DbQuery();
		}
		return "genNPlusOneWithoutInternalSpan";
	}

	@GetMapping("NPlusOneWithInternalSpan")
	public String genNPlusOneWithInternalSpan() {
		Span span = otelTracer.spanBuilder("db_access_01").startSpan();

		try {
			for (int i = 0; i < 100; i++) {
				DbQuery();
			}
		}
		finally {
			span.end();
		}
		return "genNPlusOneWithInternalSpan";
	}

	@GetMapping("GenerateSpans")
	public String generateSpans(@RequestParam(name = "uniqueSpans") long uniqueSpans) {
		for (int i = 0; i < uniqueSpans; i++) {
			GenerateSpan("GeneratedSpan_" + i);
		}

		return "Success";
	}

	private void GenerateSpan(String spanName) {
		Span span = otelTracer.spanBuilder(spanName).startSpan();
		try {
			delay(0);
		}
		finally {
			span.end();
		}
	}

	@GetMapping("GenerateSpansWithRandom")
	public ArrayList<Integer> generateSpansWithRandom(@RequestParam(name = "uniqueSpans") int uniqueSpans,
			@RequestParam(name = "min") int min, @RequestParam(name = "max") int max) {
		Random rand = new Random();
		var numberArray = IntStream.range(min, max + 1).boxed().collect(Collectors.toList());

		var resultList = new ArrayList<Integer>();

		for (int i = 0; i < uniqueSpans; i++) {
			int randomIndex = rand.nextInt(numberArray.size());
			int randomElement = numberArray.get(randomIndex);
			numberArray.remove(randomIndex);
			resultList.add(randomElement);
			GenerateSpan("GeneratedSpan_" + randomElement);
		}

		return resultList;
	}

	@GetMapping("/ScalingIssueWithoutInflux")
	public String ScalingIssueWithoutInflux(@RequestParam(name = "uniqueSuffix") String uniqueSuffix) {
		simulateScalingIssueForLoop("ScalingIssueSpanWithoutInflux" + uniqueSuffix);
		return "ScalingIssue1";
	}

	/**
	 * Second new endpoint that does exactly the same as ScalingIssue1.
	 */
	@GetMapping("/ScalingIssueInflux")
	public String scalingIssue2(@RequestParam(name = "uniqueSuffix") String uniqueSuffix) {
		simulateScalingIssueForLoop("ScalingIssueSpan" + uniqueSuffix);
		return "ScalingIssue2";
	}

	@GetMapping("/ErrorOnHttpClient")
	public String errorOnHttpClient() {
		triggerFailingHttpCall();

		return "ErrorOnHttpClient";
	}

	@GetMapping("/chatty-api")
	public String ChattyAPI() {
		callsToHttpClient();

		return "callsToHttpClient";
	}


	@GetMapping("/PerformanceAnomaly")
	public String performanceAnomaly() {
		Random random = new Random();

		for (int i = 0; i < 150; i++) {
			Span span = otelTracer.spanBuilder("SpanWithPerformanceAnomaly").startSpan();
			try {
				if (i < 110) {
					delay(100);
				} else {
					delay(1200);
				}
			} finally {
				span.end();
			}
		}

		return "PerformanceAnomaly";
	}

	@GetMapping("/largeTrace")
	public String getLargeTrace() {
		// The Spring auto-instrumentation creates the top-level SERVER span for /largeTrace.
		// We only create a child span for "GetUsersQuery".
		Span getUsersQuerySpan = otelTracer.spanBuilder("GetUsersQuery").startSpan();

		String repeatedSymbols = IntStream.range(0, 1500)
			.mapToObj(i -> "A")
			.collect(Collectors.joining());

		try (Scope scope = getUsersQuerySpan.makeCurrent()) {
			// Simulate multiple DB queries, each as a CLIENT span.
			for (int i = 1; i <= 800; i++) {
				dbQueryLarge(i, repeatedSymbols);
			}
		} finally {
			getUsersQuerySpan.end();
		}

		return "OK";
	}

	private void dbQueryLarge(int id, String repeatedSymbols) {

		// This simulates a DB query span (CLIENT).
		Span dbSpan = otelTracer.spanBuilder("query_users_by_id")
			.setSpanKind(SpanKind.CLIENT)
			.setAttribute("db.system", "postgresql")
			.setAttribute("db.statement", "SELECT * FROM users" + repeatedSymbols +" WHERE id=" + id)
			.startSpan();

		try (Scope scope = dbSpan.makeCurrent()) {
			// Simulate ~20ms of work (like DoWork in the C# code).
			Thread.sleep(20);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		} finally {
			dbSpan.end();
		}
	}

	@Async // Mark the method to run asynchronously
	public void triggerFailingHttpCall() {
		try {
			// Trigger a failing HTTP call
			restTemplate.getForObject("http://nonexistent-service/fail", String.class);
		} catch (RestClientException ex) {
			// Log or handle the exception (optional)
			System.err.println("HTTP call failed: " + ex.getMessage());
		}
	}

	@Async // Mark the method to run asynchronously
	public void callsToHttpClient() {
		try {
			// Trigger a failing HTTP call
			for (int concurrency = 1; concurrency <= 8; concurrency++) {
				restTemplate.getForObject("http://google.com", String.class);
			}
		} catch (RestClientException ex) {
			// Log or handle the exception (optional)
			System.err.println("HTTP call failed: " + ex.getMessage());
		}
	}



	private void simulateScalingIssueForLoop(String badScalingSpanName) {
		// Outer loop from 1 up to 5
		for (int concurrency = 1; concurrency <= 5; concurrency++) {

			for (int i = 0; i < concurrency; i++) {
				int finalConcurrency = concurrency;
				int index = i;

					Span spanParent = otelTracer.spanBuilder("concurrency" + finalConcurrency + "parent" + index).startSpan();

					try (Scope scope = spanParent.makeCurrent()) {
						Instant starts = Instant.now();

						// Start a span for each task
						Span span = otelTracer.spanBuilder(badScalingSpanName).startSpan();

						try {
							// Print concurrency for debugging
							System.out.println("Concurrency: " + finalConcurrency);
							busyWaitMillis(finalConcurrency);

						} finally {
							span.end();
							Instant ends = Instant.now();
							System.out.println("Span duration: " + Duration.between(starts, ends));
						}

					}finally {
						spanParent.end();
					}
			}

			// After concurrency tasks finish, create "AnotherSpan"
			Span span = otelTracer.spanBuilder("AnotherSpan").startSpan();
			try {
				delay(1000); // or LockSupport.parkNanos(1_000_000_000L) if you want the same style
			} finally {
				span.end();
			}
		}
	}





	private void DbQuery() {
		// simulate SpanKind of DB query
		// see
		// https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/semantic_conventions/database.md
		Span span = otelTracer.spanBuilder("query_users_by_id")
			.setSpanKind(SpanKind.CLIENT)
			.setAttribute("db.system", "other_sql")
			.setAttribute("db.statement", "select * from users where id = :id")
			.startSpan();

		try {
			// delay(1);
		}
		finally {
			span.end();
		}
	}

	@WithSpan
	private void doSomeWorkA(long millis) {
		delay(millis);
	}

	@WithSpan
	private void doSomeWorkB(long millis) {
		delay(millis);
	}

	@GetMapping("/bigasynctrace")
	public String bigAsyncTrace() {
		for (int i = 0; i < 200; i++) {
			// 1) Create a parent (sync) span
			Span parentSpan = otelTracer
				.spanBuilder("SyncSpan-" + i)
				.startSpan();

			// 2) Put it in scope so child spans can be properly nested
			try (Scope scope = parentSpan.makeCurrent()) {
				// Synchronous work
				delay(1);

				// 3) Capture the current context (which has the parent span)
				Context parentContext = Context.current();

				parentSpan.end();

				// 4) Asynchronously create a child span
				triggerAsyncSpan(i, parentContext);

			}
		}
		return "BigAsyncTrace";
	}

	/**
	 * Asynchronous child-span creation.
	 * We re-activate the parent's context inside this method so that
	 * the child span is a child of the SyncSpan.
	 */
	@Async
	public void triggerAsyncSpan(int i, Context parentContext) {
		// Re-activate the parent context in this async thread
		try (Scope scope = parentContext.makeCurrent()) {
			// 1) Create the child (async) span
			Span childSpan = otelTracer
				.spanBuilder("AsyncSpan-" + i)
				.startSpan();
			try {
				// 2) Simulate async work
				delay(10);
			} finally {
				// 3) End the child span
				childSpan.end();
			}
		}
	}

	private static void delay(long millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException e) {
			Thread.interrupted();
		}
	}

	private void busyWaitMillis(long ms) {
		long waitNanos = ms * 1_000_000L;
		long startTime = System.nanoTime();
		while (System.nanoTime() - startTime < waitNanos) {
			// Spin: do nothing but burn CPU
		}
	}

}
