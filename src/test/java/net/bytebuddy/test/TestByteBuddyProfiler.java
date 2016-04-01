package net.bytebuddy.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Callable;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.Before;
import org.junit.Test;

public class TestByteBuddyProfiler {

	private static String signature;
	private static int argument;
	private static long executionTime;

	@Before
	public void setUp() throws Exception {
		ByteBuddyAgent.install();
	}

	@Test
	public void testProfileMethod() {
		// makes sure that Byte Buddy has to retransform the class
		// (but profiling should also work when the class is not yet loaded!)
		ProfiledClass.makeSureClassIsLoaded();

		// TODO does not seem to apply the transformation
		new AgentBuilder.Default()
				.with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
				.with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
				.with(AgentBuilder.TypeStrategy.Default.REDEFINE)
				.type(ElementMatchers.named(ProfiledClass.class.getName()))
				.transform((builder, typeDescription, classLoader) -> builder
						.method(ElementMatchers.any())
						.intercept(MethodDelegation.to(ProfilingInterceptor.class)))
				.installOnByteBuddyAgent();

		new ProfiledClass().profiledMethod(1);

		assertEquals("net.bytebuddy.test.TestByteBuddyProfiler$ProfiledClass.profiledMethod", signature);
		assertEquals(1, argument);
		assertTrue(executionTime > 0);
	}

	public static class ProfiledClass{
		public static void makeSureClassIsLoaded() {
		}

		public void profiledMethod(int i) {
		}
	}

	public static class ProfilingInterceptor {

		@RuntimeType
		public static Object profile(@Origin String signature, @SuperCall Callable<?> zuper) throws Exception {
			// TODO argument
			// TestByteBuddyProfiler.argument = arg;
			long start = System.nanoTime();
			// TODO customize signature (see assert)
			TestByteBuddyProfiler.signature = signature;
			try {
				return zuper.call();
			} finally {
				TestByteBuddyProfiler.executionTime = System.nanoTime() - start;
			}
		}
		/*
		@RuntimeType
		public static Object profile(@Origin(cacheMethod = true) Method method, @Origin Class clazz, @SuperCall Callable<?> zuper) throws Exception {
			String signature = clazz.toString() + method.toString();
			long start = System.nanoTime();
			TestByteBuddyProfiler.signature = signature;
			try {
				return zuper.call();
			} finally {
				executionTime = System.nanoTime() - start;
			}
		}*/
	}
}
