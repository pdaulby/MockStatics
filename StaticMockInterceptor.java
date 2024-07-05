
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.exceptions.base.MockitoException;
import org.mockito.verification.VerificationMode;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings({"unchecked", "OptionalUsedAsFieldOrParameterType"})
public class StaticMockInterceptor implements InvocationInterceptor {
    private static final Map<Class<?>, MockedStatic<?>> classMockedStaticMap = new HashMap<>();
    private Optional<Method> beforeEach = Optional.empty();

    public static <T> void verifyStatic(Class<T> className, MockedStatic.Verification verification){
        getMockedStatic(className).verify(verification);
    }
    public static <T> void verifyStatic(Class<T> className, MockedStatic.Verification verification, VerificationMode verificationMode){
        getMockedStatic(className).verify(verification, verificationMode);
    }
    public static <T> void verifyNoStaticInteractions(Class<T> className){
        getMockedStatic(className).verifyNoInteractions();
    }
    public static <T> void verifyNoMoreStaticInteractions(Class<T> className){
        getMockedStatic(className).verifyNoMoreInteractions();
    }
    public static <T> MockedStatic<T> getMockedStatic(Class<T> className){
        if (!classMockedStaticMap.containsKey(className)) {
            fail("class " + className + " has not been mocked.");
        }
        return (MockedStatic<T>) classMockedStaticMap.get(className);
    }
    
    @Override
    public void interceptBeforeEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        if (invocationContext.getExecutable().isAnnotationPresent(MockStatics.class)){
            beforeEach = Optional.of(invocationContext.getExecutable());
            invocation.skip();
            return;
        }
        beforeEach = Optional.empty();
        InvocationInterceptor.super.interceptBeforeEachMethod(invocation, invocationContext, extensionContext);
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        Class<?>[] classes = Stream.concat(
                getTestMocks(beforeEach), 
                getTestMocks(extensionContext.getTestMethod()))
                .toArray(Class[]::new);
        
        classMockedStaticMap.clear();
        try {
            wrapTestInvocation(classes, 0, invocation, invocationContext, extensionContext);
        } catch (MockitoException e) {
            throw e.getCause(); // gives a cleaner error message and stacktrace
        }
    }

    private Stream<Class<?>> getTestMocks(Optional<Method> testMethod) {
        return testMethod
                .map(method -> method.getAnnotation(MockStatics.class))
                .map(MockStatics::value)
                .map(Arrays::stream)
                .orElse(Stream.empty());
    }

    private void wrapTestInvocation(Class<?>[] classes, int count, Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        if (count >= classes.length) {
            beforeEach.ifPresent((before) -> ReflectionUtils.invokeMethod(before, invocationContext.getTarget().get()));
            InvocationInterceptor.super.interceptTestMethod(invocation, invocationContext, extensionContext);
            beforeEach = Optional.empty(); //probably not necessary, just for safety.
            classMockedStaticMap.clear();
            return;
        }
        try (MockedStatic<?> mockedStatic = Mockito.mockStatic(classes[count])) {
            classMockedStaticMap.put(classes[count], mockedStatic);
            wrapTestInvocation(classes, count + 1, invocation, invocationContext, extensionContext);
        }
    }
}
