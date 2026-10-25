package org.example.test;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Base class for isolated unit test suites.
 *
 * <p><b>Purpose:</b><br>
 * A unit test verifies individual components or classes in complete isolation from external
 * dependencies like databases or network services, running rapidly and reliably. This base
 * class streamlines unit testing by automatically setting up test classification tags and
 * initializing Mockito mock objects.
 *
 * <p><b>How it works:</b>
 * <ul>
 *   <li><b>Tagging:</b> Annotated with {@link Tag @Tag("unit")} so that test runners and build
 *       tools can identify and execute all unit tests as a standalone group.</li>
 *   <li><b>Mocking Support:</b> Annotated with {@link ExtendWith @ExtendWith(MockitoExtension.class)},
 *       which automatically initializes Mockito annotations (such as {@code @Mock} and
 *       {@code @InjectMocks}) before each test method runs, eliminating repetitive boilerplate code.</li>
 * </ul>
 *
 * <p><b>How to use it effectively:</b>
 * <ul>
 *   <li>Extend this class when writing unit tests that require mock collaborators:
 *       <pre>{@code
 *       public class OrderServiceTest extends UnitTest {
 *           @Mock
 *           private PaymentGateway paymentGateway;
 *
 *           @InjectMocks
 *           private OrderService orderService;
 *
 *           @Test
 *           void testProcessOrder() {
 *               // paymentGateway mock is automatically initialized and ready to use
 *               orderService.processOrder(123);
 *           }
 *       }
 *       }</pre>
 *   </li>
 *   <li>Run only unit tests during rapid local feedback cycles using your build tool:
 *       {@code ./gradlew test -PincludeTags=unit}.</li>
 * </ul>
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
public abstract class UnitTest {
}