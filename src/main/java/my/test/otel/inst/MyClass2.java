package my.test.otel.inst;

public class MyClass2 {

	public void myMethodInMyClass2(){
		System.out.printf("");
	}


	public void myMethodInMyClass2(String str){
		System.out.printf(str);
	}

	public void notInstrumented(){
		System.out.println("");
	}

}
