package org.springframework.samples.petclinic;


import io.opentelemetry.instrumentation.annotations.WithSpan;

public class MyMain {

	public static void main(String[] args) {
		System.out.println("aaaaaaaaaaaaaaaaaa");
	}


	@WithSpan
	public void test(){

	}
}
