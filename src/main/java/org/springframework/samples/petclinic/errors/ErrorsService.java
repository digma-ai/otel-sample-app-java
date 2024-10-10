package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.apache.coyote.BadRequestException;

public class ErrorsService
{
	private final Tracer otelTracer;
	public ErrorsService(Tracer otelTracer)
	{
		this.otelTracer = otelTracer;
	}
	public <T extends Exception> void handleException(Class<T> exceptionClass, String message) {
		Span span = otelTracer.spanBuilder("handleException").startSpan();

		try {
			Utils.throwException(exceptionClass, message);
		}
		catch (Exception e) {
			span.recordException(e);
			span.setStatus(StatusCode.ERROR);
		}
		finally {
			span.end();
		}
	}

	public void GenerateMultipleErrors()
	{
		for (int i = 0; i < 20; i++) {
			RandomErrorThrower(i);
		}
	}

	public void GenerateMultipleUnexpectedErrors() {
		for (int i = 0; i < 19; i++) {
			Span span = otelTracer.spanBuilder("UnexpectedErrorThrower").startSpan();

			try {
				RandomErrorThrower.throwUnexpectedError(i);
			} catch (Exception e) {
				span.recordException(e);
				span.setStatus(StatusCode.ERROR);
			} finally {
				span.end();
			}
		}
	}

	private void RandomErrorThrower(int errorType)
	{
		Span span = otelTracer.spanBuilder("RandomErrorThrower").startSpan();

		try {
			RandomErrorThrower.throwError(errorType);
		}
		catch (Exception e) {
			span.recordException(e);
			span.setStatus(StatusCode.ERROR);
		}
		finally {
			span.end();
		}
	}
	public void handleUnsupportedOperationException()
	{
		Span span = otelTracer.spanBuilder("handleUnsupportedOperationException").startSpan();

		try {
			Utils.ThrowUnsupportedOperationException();
		}
		catch (Exception e) {
			span.recordException(e);
			span.setStatus(StatusCode.ERROR);
		}
		finally {
			span.end();
		}
	}

	@WithSpan
	public void methodA() throws BadRequestException {
		methodB();
	}

	@WithSpan
	public void methodB() throws BadRequestException {
		 Utils.ThrowBadRequestException();
	}

	public void methodC() {
		Span span = otelTracer.spanBuilder("methodC").startSpan();

		try {
			Utils.ThrowUnsupportedOperationException();
		}
		catch (Exception e) {
			span.recordException(e);
			span.setStatus(StatusCode.ERROR);
		}
		finally {
			span.end();
		}
	}
}


