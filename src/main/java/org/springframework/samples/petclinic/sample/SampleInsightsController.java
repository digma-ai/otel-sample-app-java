package org.springframework.samples.petclinic.sample;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/SampleInsights")
public class SampleInsightsController {

	@GetMapping("/HighUsage")
	public String handleHighUsage() {
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
