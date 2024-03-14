package my.test.otel.inst;

public class MyClass {

	public void myMethod1(){
		System.out.printf("aa");
	}

	public void myMethod2(){
		System.out.printf("aa");

		new MyClass2().myMethodInMyClass2();

	}

}
