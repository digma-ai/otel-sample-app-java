package org.springframework.samples.petclinic.sample;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.petclinic.system.AppException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.management.openmbean.InvalidOpenTypeException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.*;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping("/SampleInsights")
public class SampleInsightsController implements InitializingBean {

	@Autowired
	private OpenTelemetry openTelemetry;
	private DummyService1 dummyService1;

	private Tracer otelTracer;
	private ExecutorService executorService;

	@Override
	public void afterPropertiesSet() throws Exception {
		this.otelTracer = openTelemetry.getTracer("SampleInsightsController");
		this.executorService = Executors.newFixedThreadPool(5);
		this.dummyService1 = new DummyService1(openTelemetry);

	}

	@GetMapping("ErrorsScenarios")
	public void ErrorsScenarios() {
		Span span = Span.current();
		try {
			throw new AppException("on current span");
		} catch (AppException e) {
			span.recordException(e);
			span.setStatus(StatusCode.ERROR);
		}

		try {
			dummyService1.Check();
		}
		catch (InvalidOpenTypeException e)
		{
			throw e;
		}
	}
	@GetMapping("DemoMultiSourceError")
	public void DemoMultiSourceError() {
		dummyService1.ThrowFromDifferentSources();
	}

	@GetMapping("DemoRethrow")
	public void DemoRethrow() {
		dummyService1.DemoRethrow();
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
		} catch (AppException e) {
			span.recordException(e);
			span.setStatus(StatusCode.ERROR);
		} finally {
			span.end();
		}
	}

	@GetMapping("ErrorRecordedOnCurrentSpan")
	public String genErrorRecordedOnCurrentSpan() {
		Span span = Span.current();
		try {
			throw new AppException("on current span");
		} catch (AppException e) {
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
		} catch (AppException e) {
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
		} finally {
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

	private void GenerateSpan(String spanName){
		Span span = otelTracer.spanBuilder(spanName).startSpan();
		try {
			delay(0);
		}
		finally {
			span.end();
		}
	}

	@GetMapping("GenerateSpansWithRandom")
	public ArrayList<Integer> generateSpansWithRandom(@RequestParam(name = "uniqueSpans") int uniqueSpans, @RequestParam(name = "min")int min, @RequestParam(name = "max")int max) {
		Random rand = new Random();
		var numberArray = IntStream.range(min, max + 1)
			.boxed()
			.collect(Collectors.toList());

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
		} finally {
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

	private static void delay(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.interrupted();
		}
	}

}
