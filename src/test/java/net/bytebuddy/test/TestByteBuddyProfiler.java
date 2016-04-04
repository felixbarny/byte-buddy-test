package net.bytebuddy.test;

import static java.lang.System.nanoTime;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.Before;
import org.junit.Test;

public class TestByteBuddyProfiler {

	private static String signature;
	private static int returnValue;
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
						.visit(Advice.to(ProfilingAdvice.class).on(ElementMatchers.any())))
				.installOnByteBuddyAgent();

		new ProfiledClass().profiledMethod(1);

		assertEquals("net.bytebuddy.test.TestByteBuddyProfiler$ProfiledClass.profiledMethod", signature);
		assertEquals(1, returnValue);
		assertTrue(executionTime > 0);
	}

	public static void someMethod() {
		System.out.println("called some method");
	}

	public static class ProfiledClass{
		public static void makeSureClassIsLoaded() {
		}

		public int profiledMethod(int i) {
			return i;
		}
	}

	private static class ProfilingAdvice {
		@Advice.OnMethodEnter
		public static long enter(@Advice.Origin("#t.#m") String signature) {
			TestByteBuddyProfiler.signature = signature;
			return nanoTime();
		}

		@Advice.OnMethodExit
		public static void exit(@Advice.Return long value) {
			// TODO how do I capture the return value when value is the added local variable?
			executionTime = System.nanoTime() - value;
		}
	}
}
