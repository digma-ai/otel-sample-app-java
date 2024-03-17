package my.test.otel.inst;

import my.test.otel.inst2.MyClass2;

public class MyClass {

	public void calledFromInitFindForm(){
		System.out.printf("aa");
	}

	public void calledFromProcessFindForm(){
		System.out.printf("aa");

		//should be bottleneck
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		new MyClass2().calledFromMyClass();

	}

}
