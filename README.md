Example Usage

    @ExtendWith({MockitoExtension.class, StaticMockInterceptor.class})
    public class SomeClassTest {

        @Test
        @MockStatic(LocalDateTime.class)
        void TestSomething() {
            when(LocalDateTime.now()).thenReturn(LocalDateTime.parse("2015-08-04T10:11:30"));

            //test go here

            StaticMockInterceptor.verifyStatic(LocalDateTime.class, () -> LocalDateTime.now());
        }
