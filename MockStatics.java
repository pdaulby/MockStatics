import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Usage guide:
 * Annotate a test class with @ExtendWith({MockitoExtension.class, StaticMockInterceptor.class})
 * Annotate a @BeforeEach or @Test method with @MockStatics(ClassName.class)
 * To mock the static method, call when(ClassName.StaticMethod(...)).thenReturn(...)
 * To verify a static mock, call StaticMockInterceptor.verifyStatic(Classname.class, ()->ClassName.StaticMethod(...))
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MockStatics {
    Class<?>[] value();
}
