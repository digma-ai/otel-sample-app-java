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

import com.zaxxer.hikari.HikariConfig;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.Installer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

	public static void main(String[] args) {
		var micrometerAttributes = new StringBuilder("");
		System.getenv().forEach(new BiConsumer<String, String>() {
			@Override
			public void accept(String k, String v) {
				if (k.startsWith("MANAGEMENT_OPENTELEMETRY_RESOURCE")){
					micrometerAttributes.append(k + "=" + v).append(",");
				}
			}
		});

		System.out.println("micrometer resource attributes = "+micrometerAttributes);
		System.out.println("otel resource attributes = "+System.getenv("OTEL_RESOURCE_ATTRIBUTES"));
		SpringApplication.run(PetClinicApplication.class, args);



		new Thread(new Runnable() {
			@Override
			public void run() {

				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}

				System.out.println("ByteBuddyAgent Installer class loader is "+Installer.class.getClassLoader());
				System.out.println("HikariConfig class loader is "+ HikariConfig.class.getClassLoader());


				Instrumentation instrumentation = null;
				try {
					Class<?> installer = Class.forName("net.bytebuddy.agent.Installer", true, ClassLoader.getSystemClassLoader());
					instrumentation = (Instrumentation) installer.getMethod("getInstrumentation").invoke(null);

				} catch (Exception e) {
					String msg = e.toString();
					if (e instanceof InvocationTargetException && ((InvocationTargetException) e).getTargetException() != null){
						msg = ((InvocationTargetException) e).getTargetException().toString();
					}
					System.out.println("error trying to get instrumentation from bytebuddy Installer "+msg);
				}

				if (instrumentation != null){
					System.out.println("got instrumentation from Installer");
				}else{
					System.out.println("installing byte buddy agent");
					instrumentation = ByteBuddyAgent.install();
					System.out.println("got inst after agent install "+instrumentation);
				}
			}
		}).start();

	}

}
