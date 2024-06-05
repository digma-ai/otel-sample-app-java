package org.springframework.samples.petclinic.sample;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.samples.petclinic.system.AppException;

import javax.management.openmbean.InvalidOpenTypeException;
import java.util.Random;

public class DummyService1 {

	private OpenTelemetry openTelemetry;
	private Tracer otelTracer;
	public DummyService1(OpenTelemetry openTelemetry){
		this.openTelemetry = openTelemetry;
		this.otelTracer = openTelemetry.getTracer("OwnerController");
	}


	private void InnerCheck() {
		Span span = this.otelTracer.spanBuilder("InnerCheck").startSpan();

		try {
			throw new InvalidOpenTypeException("some ex2");
		}
		catch (InvalidOpenTypeException e) {
			span.recordException(e);
			span.setStatus(StatusCode.ERROR);
			throw e;
		} finally {
			span.end();
		}
	}
	public void Check()
	{
		Span span = this.otelTracer.spanBuilder("Check").startSpan();

		try {
			InnerCheck();
			//throw new InvalidOpenTypeException("some ex1");
		}
		catch (InvalidOpenTypeException e) {
			span.recordException(e);
			span.setStatus(StatusCode.ERROR);
			throw e;
		} finally {
			span.end();
		}
	}

	public void ThrowArgumentExceptionSource1()
	{
		Raise("error1");
	}

	public void ThrowArgumentExceptionSource2()
	{
		Raise("error2");
	}
	private final Random random = new Random();
	public void ThrowFromDifferentSources()
	{
		boolean randomBool = this.random.nextInt(2) == 0;
		if(randomBool){
			ThrowArgumentExceptionSource1();
		}
		else{
			ThrowArgumentExceptionSource2();
		}
	}

	public void DemoRethrow(){
		try{
			Raise("asd");
		}
		catch (AppException e)
		{
			throw new RuntimeException("some message", e);
		}
	}
	public void Raise(String msg){
		throw new AppException(msg);
	}
}
