package org.springframework.samples.petclinic.errors;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

public class RandomErrorThrower {

	// Custom exception classes
	static class CustomException1 extends Exception {
	}

	static class CustomException2 extends Exception {
	}

	static class CustomException3 extends Exception {
	}

	static class CustomException4 extends Exception {
	}

	static class CustomException5 extends Exception {
	}

	static class CustomException6 extends Exception {
	}

	static class CustomException7 extends Exception {
	}

	static class CustomException8 extends Exception {
	}

	static class CustomException9 extends Exception {
	}

	static class CustomException10 extends Exception {
	}

	static class CustomException11 extends Exception {
	}

	static class CustomException12 extends Exception {
	}

	static class CustomException13 extends Exception {
	}

	static class CustomException14 extends Exception {
	}

	static class CustomException15 extends Exception {
	}

	static class CustomException16 extends Exception {
	}

	static class CustomException17 extends Exception {
	}

	static class CustomException18 extends Exception {
	}

	static class CustomException19 extends Exception {
	}

	static class CustomException20 extends Exception {
	}

	public static void throwUnexpectedError(int errorType) throws Exception {
		switch (errorType) {
			case 0:
				throw new NullPointerException();
			case 1:
				throw new ArrayIndexOutOfBoundsException();
			case 2:
				throw new ClassCastException();
			case 3:
				throw new IllegalArgumentException();
			case 4:
				throw new IllegalStateException();
			case 5:
				throw new ArithmeticException();
			case 6:
				throw new ConcurrentModificationException();
			case 7:
				throw new UnsupportedOperationException();
			case 8:
				throw new IndexOutOfBoundsException();
			case 9:
				throw new NoSuchElementException();
			case 10:
				throw new NumberFormatException();
			case 11:
				throw new StackOverflowError();
			case 12:
				throw new OutOfMemoryError();
			case 13:
				throw new ClassNotFoundException();
			case 14:
				throw new IllegalMonitorStateException();
			case 15:
				throw new SecurityException();
			case 16:
				throw new NoClassDefFoundError();
			case 17:
				throw new AssertionError();
			case 18:
				throw new TypeNotPresentException("com.example.MyMissingClass", new ClassNotFoundException());
			default:
				throw new Exception("Unexpected error type");
		}
	}

	public static void throwError(int errorType) throws Exception {
		switch (errorType) {
			case 0:
				throw new CustomException1();
			case 1:
				throw new CustomException2();
			case 2:
				throw new CustomException3();
			case 3:
				throw new CustomException4();
			case 4:
				throw new CustomException5();
			case 5:
				throw new CustomException6();
			case 6:
				throw new CustomException7();
			case 7:
				throw new CustomException8();
			case 8:
				throw new CustomException9();
			case 9:
				throw new CustomException10();
			case 10:
				throw new CustomException11();
			case 11:
				throw new CustomException12();
			case 12:
				throw new CustomException13();
			case 13:
				throw new CustomException14();
			case 14:
				throw new CustomException15();
			case 15:
				throw new CustomException16();
			case 16:
				throw new CustomException17();
			case 17:
				throw new CustomException18();
			case 18:
				throw new CustomException19();
			case 19:
				throw new CustomException20();
			default:
				throw new Exception("Unexpected error type");
		}
	}


	public static void generateAndThrowException(String className, String message) throws Throwable {
		// Dynamically generate the class that extends Throwable
		Class<?> exceptionClass = createThrowableClass(className);

		// Create an instance of the dynamically generated class
		Constructor<?> constructor = exceptionClass.getConstructor(String.class);
		Throwable exceptionInstance = (Throwable) constructor.newInstance(message);

		// Throw the exception
		throw exceptionInstance;
	}

	private static Class<?> createThrowableClass(String className) throws Exception {
		// Define the Java source code for the new exception class
		String sourceCode =
			"public class " + className + " extends java.lang.Exception { " +
				"    public " + className + "(String message) { " +
				"        super(message); " +
				"    } " +
				"} ";

		// Write the source code to a .java file
		String fileName = className + ".java";
		try (FileWriter fileWriter = new FileWriter(fileName)) {
			fileWriter.write(sourceCode);
		}

		// Compile the Java source file using the JavaCompiler API
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		int compilationResult = compiler.run(null, null, null, fileName);
		if (compilationResult != 0) {
			throw new RuntimeException("Compilation failed.");
		}

		// Load the compiled class into the JVM
		File file = new File(".");
		URL[] urls = new URL[]{file.toURI().toURL()};
		URLClassLoader classLoader = new URLClassLoader(urls);
		return classLoader.loadClass(className);
	}

}
