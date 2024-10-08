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

package org.springframework.samples.petclinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportRuntimeHints;

import java.sql.SQLException;
import java.util.function.BiConsumer;

/**
 * PetClinic Spring Boot Application.
 *
 * @author Dave Syer
 *
 */
@SpringBootApplication
@ImportRuntimeHints(PetClinicRuntimeHints.class)
public class PetClinicApplication {

	public static void main(String[] args) throws SQLException {
		var micrometerAttributes = new StringBuilder("");
		System.getenv().forEach(new BiConsumer<String, String>() {
			@Override
			public void accept(String k, String v) {
				if (k.startsWith("MANAGEMENT_OPENTELEMETRY_RESOURCE")) {
					micrometerAttributes.append(k + "=" + v).append(",");
				}
			}
		});

		System.out.println("micrometer resource attributes = " + micrometerAttributes);
		System.out.println("otel resource attributes = " + System.getenv("OTEL_RESOURCE_ATTRIBUTES"));
		ApplicationContext applicationContext = SpringApplication.run(PetClinicApplication.class, args);
	}

}
