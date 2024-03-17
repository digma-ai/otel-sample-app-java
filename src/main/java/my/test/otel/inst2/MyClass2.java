package my.test.otel.inst2;

public class MyClass2 {

	public void calledFromMyClass() {
		System.out.printf("");
	}

	public void calledFromFindOwner() {
		System.out.printf("");
	}


	public void myThrowsExceptionMethod() {
		throw new UnsupportedOperationException("test instrument myThrowsExceptionMethod");
	}

	public void calledFromProcessNewVisitForm() {
	}

	public void calledFromInitNewVisitForm() {
	}
}
