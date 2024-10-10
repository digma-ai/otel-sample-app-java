package org.springframework.samples.petclinic.errors;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@RestController
@RequestMapping("/Errors")
public class ErrorsController implements InitializingBean {

	@Autowired
	private AsyncService asyncService;

	@Autowired
	private OpenTelemetry openTelemetry;

	private Tracer otelTracer;

	private ErrorsService errorsService;

	@Override
	public void afterPropertiesSet() throws Exception {
		this.otelTracer = openTelemetry.getTracer("SampleInsightsController");
		this.errorsService = new ErrorsService(this.otelTracer);
	}

	//generate unhandled exception of type IllegalStateException
	@GetMapping("error1")
	public void error1() throws IllegalStateException {
		Utils.throwException(IllegalStateException.class,"some message");
	}
	//handled exception of type IllegalStateException
	@GetMapping("error2")
	public void error2() {
		errorsService.handleException(IllegalStateException.class,"some message");
	}

	//generate unhandled exception of type UnsupportedOperationException
	@GetMapping("error3")
	public void error3(){
		Utils.ThrowUnsupportedOperationException();
	}

	//handled exception of type UnsupportedOperationException
	@GetMapping("error4")
	public void error4() {
		errorsService.handleUnsupportedOperationException();
	}

	@GetMapping("error5")
	public void error5() throws BadRequestException {
		Utils.ThrowBadRequestException();
	}

	@GetMapping("error6")
	public void error6() throws BadRequestException {
		errorsService.GenerateMultipleErrors();
	}
	static Random random = new Random();
	private static final Logger logger = LoggerFactory.getLogger(ErrorsController.class);

	@GetMapping("error7")
	public void error7() throws Throwable {
		for (int i = 0; i < 100; i++) {
			Span span = otelTracer.spanBuilder("error7").startSpan();
			try {
				RandomErrorThrower.generateAndThrowException("exception"+random.nextInt(3000) + 1,"message "+i);
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

	@GetMapping("error8")
	public void error8() throws IllegalStateException, BadRequestException {
		errorsService.methodA();
	}

	@GetMapping("error9")
	public void error9() {
		errorsService.methodC();
	}
	@GetMapping("error10")
	public void error10() throws BadRequestException {
		Utils.ThrowBadRequestException();
	}

	@GetMapping("error11")
	public void error11() {
		errorsService.GenerateMultipleUnexpectedErrors();
	}

	//ThrowBadRequestException
	@GetMapping("/run-async")
	public void runAsyncTask() {
		asyncService.runMultipleAsyncTasks();
	}

}
