/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.system;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.micrometer.core.annotation.TimedMetrics;

/**
 * Controller used to showcase what happens when an exception is thrown.
 * This is a demonstration endpoint that illustrates error handling capabilities.
 *
 * @author Michael Isvy
 * <p/>
 * Also see how a view that resolves to "error" has been added ("error.html").
 */
@Controller
@Api(tags = "Crash Controller", description = "Demonstration endpoint for error handling")
@TimedMetrics(exclude = true)import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.micrometer.core.annotation.TimedMetrics;

/**
 * Controller used to showcase what happens when an exception is thrown.
 * This is a demonstration endpoint for testing error handling.
 */
@Api(tags = "Crash Controller", description = "Demonstration endpoint for error handling")
class CrashController {

	/**
	 * Triggers a sample exception to demonstrate error handling.
	 * @return Never returns as it always throws an exception
	 * @throws RuntimeException Always thrown to demonstrate error handling
	 */
	@GetMapping("/oups")
	@ApiOperation(value = "Trigger sample exception", notes = "Throws a runtime exception to demonstrate error handling")
	@TimedMetrics(exclude = true)
	public String triggerException() {
		throw new RuntimeException(
				"Expected: controller used to showcase what " + "happens when an exception is thrown");
	}

}