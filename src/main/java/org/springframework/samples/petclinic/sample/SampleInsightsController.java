package org.springframework.samples.petclinic.sample;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/SampleInsights")
public class SampleInsightsController {

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

	@GetMapping("/HighUsage")
	public String genHighUsage() {
		delay(5);
		return "highUsage";
	}

	private static void delay(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.interrupted();
		}
	}

}
