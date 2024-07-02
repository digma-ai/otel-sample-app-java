package org.springframework.samples.petclinic.sample;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.petclinic.system.AppException;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.*;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping("/SampleInsights")
public class SampleInsightsController implements InitializingBean {

	@Autowired
	private OpenTelemetry openTelemetry;
	private Random random = new Random(7);

	private Tracer otelTracer;
	private ExecutorService executorService;

	@Override
	public void afterPropertiesSet() throws Exception {
		this.otelTracer = openTelemetry.getTracer("SampleInsightsController");
		this.executorService = Executors.newFixedThreadPool(5);
	}

	@GetMapping("/Process/{count}")
	public String Process(@PathVariable("count") int count){
		SubProcess(0, count);
		return "done";
	}

	private void SubProcess(int index,  int children){
		Span span = otelTracer.spanBuilder("SubProcess#"+index).startSpan();
		try (Scope scope = span.makeCurrent()) {
			int childDurationNano = random.nextInt(100_000, 999_999) / children;
			for (int i = 0; i < children; i++) {
				double queryDurationNano = random.nextDouble(childDurationNano*0.8,childDurationNano);
				DbQuery("table_"+i, (int)queryDurationNano);
			}
		} catch (InterruptedException e) {
			span.recordException(e);
			span.setStatus(StatusCode.ERROR);
        } finally {
			span.end();
		}
	}

	@WithSpan(kind = SpanKind.CLIENT)
	private void DbQuery(String tableName, int queryDurationNano) throws InterruptedException {
		var span = Span.current();
		span.setAttribute("db.system", "other_sql");
		span.setAttribute("db.statement", "select * "+tableName+" users where id = :id");
		Thread.sleep(0, queryDurationNano);
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

	@GetMapping("EP1")
	public String ep1(){
		for (int i = 0; i < 2; i++) {
			DbQuery();
		}
		return "EP1";
	}

	@GetMapping("EP2")
	public String ep2(){
		for (int i = 0; i < 3; i++) {
			DbQuery();
		}
		return "EP2";
	}

	@WithSpan(kind = SpanKind.CLIENT)
	private void DbQuery() {
		var span = Span.current();
		span.setAttribute("db.system", "other_sql");
		span.setAttribute("db.statement", "select * from users where id = :id");
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
		for (int i = 0; i < uniqueSpans / 4; i++) {
			GenerateSpan("Generated" + i + "Span");
		}

		for (int i = 0; i < uniqueSpans / 4; i++) {
			GenerateSpan2("Generated" + i + "Span");
		}

		for (int i = 0; i < uniqueSpans / 4; i++) {
			GenerateSpan3("Generated" + i + "Span");
		}

		return "Success";
	}

	private void GenerateSpan(String spanName){
		Span span = otelTracer.spanBuilder(spanName).startSpan();
		try {
		}
		finally {
			span.end();
		}
	}

	private void GenerateSpan2(String spanName){
		Span span = otelTracer.spanBuilder(spanName).startSpan();
		try {
		}
		finally {
			span.end();
		}
	}

	private void GenerateSpan3(String spanName){
		Span span = otelTracer.spanBuilder(spanName).startSpan();
		try {
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
