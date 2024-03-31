package my.extended.pkg;

public class MyErrorClass {

	public void doSomethingWithBottleneck(){

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		System.out.println("do something");
	}

	public void doSomethingWithError(){
		throw new RuntimeException("my error");
	}
}
