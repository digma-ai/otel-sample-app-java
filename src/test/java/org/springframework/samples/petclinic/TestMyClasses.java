package org.springframework.samples.petclinic;

import my.test.otel.inst.MyClass;
import my.test.otel.inst2.MyClass2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestMyClasses {

	@Test
	public void testMyClasses(){

		MyClass myClass = new MyClass();

		myClass.myMethod1();
		myClass.myMethod2();

		MyClass2 myClass2 = new MyClass2();
		myClass2.myMethodInMyClass2();
		myClass2.myMethodInMyClass2("test");
		myClass2.notInstrumented();
	}


	public void testThrowing(){
		UnsupportedOperationException thrown = Assertions.assertThrows(UnsupportedOperationException.class, () -> {
			MyClass2 myClass2 = new MyClass2();
			myClass2.myThrowsExceptionMethod();
		});

		Assertions.assertEquals("test instrument myThrowsExceptionMethod", thrown.getMessage());
	}

}
