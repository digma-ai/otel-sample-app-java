package my.test.otel.inst2;

public class MyClass2 {

	public void myMethodInMyClass2(){
		System.out.printf("");
	}


	public void myMethodInMyClass2(String str){
		System.out.printf(str);
	}

	public void myNotCalledFromProduction(){
		System.out.println("");
	}

	public void myThrowsExceptionMethod(){
		throw new UnsupportedOperationException("test instrument myThrowsExceptionMethod");
	}

}
