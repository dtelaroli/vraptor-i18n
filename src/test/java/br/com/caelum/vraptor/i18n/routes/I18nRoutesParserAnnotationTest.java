package br.com.caelum.vraptor.i18n.routes;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import br.com.caelum.vraptor.Controller;
import br.com.caelum.vraptor.Path;
import br.com.caelum.vraptor.controller.DefaultBeanClass;
import br.com.caelum.vraptor.core.Converters;
import br.com.caelum.vraptor.http.EncodingHandler;
import br.com.caelum.vraptor.http.ParameterNameProvider;
import br.com.caelum.vraptor.http.ParanamerNameProvider;
import br.com.caelum.vraptor.http.route.DefaultRouteBuilder;
import br.com.caelum.vraptor.http.route.JavaEvaluator;
import br.com.caelum.vraptor.http.route.NoTypeFinder;
import br.com.caelum.vraptor.http.route.Route;
import br.com.caelum.vraptor.http.route.Router;
import br.com.caelum.vraptor.proxy.JavassistProxifier;
import br.com.caelum.vraptor.proxy.Proxifier;

public class I18nRoutesParserAnnotationTest {
	
	private Proxifier proxifier;
	private @Mock Converters converters;
	private NoTypeFinder typeFinder;
	private @Mock Router router;
	private ParameterNameProvider nameProvider;
	private @Mock EncodingHandler encodingHandler;

	private I18nRoutesParser parser;
	private @Mock RoutesResources routesResource;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);

		this.proxifier = new JavassistProxifier();
		this.typeFinder = new NoTypeFinder();
		this.nameProvider = new ParanamerNameProvider();

		when(router.builderFor(anyString())).thenAnswer(new Answer<DefaultRouteBuilder>() {

			@Override
			public DefaultRouteBuilder answer(InvocationOnMock invocation) throws Throwable {
				return new DefaultRouteBuilder(proxifier, typeFinder, converters, nameProvider, new JavaEvaluator(), (String) invocation.getArguments()[0],encodingHandler);
			}
		});
		
		when(routesResource.getAvailableBundles()).thenReturn(new ArrayList<ResourceBundle>());

		parser = new I18nRoutesParser(router, routesResource);
	}
	
	@Controller
	@Path("/prefix") 
	@I18nPaths({ @I18nPath(locale = "es-es", path = "/prefijo") })
	public static class AnnotatedController {
		public void withoutPath() {
		}
		@Path("/absolutePath")
		@I18nPaths({ @I18nPath(locale = "es-es", path = "/absoluto") })
		public void withAbsolutePath() {
		}
	}
	
	@Controller
	@Path("/prefix") 
	@I18nPath(locale = "es-es", path = "/prefijo")
	public static class SingleAnnotatedController {
		@Path("/absolutePath")
		@I18nPath(locale = "es-es", path = "/absoluto")
		public void withAbsolutePath() {
		}
	}

	@Controller
	public static class ConventionController {
		public void withoutPath() {
		}
		@Path("/absolutePath")
		@I18nPaths({ @I18nPath(locale = "es-es", path = "/absoluto") })
		public void withAbsolutePath() {
		}
	}

	
	@Test
	public void shouldCanHandleAnnotatedControllerMethodWithoutPathWithoutPrefix() {
		List<Route> routes = parser.rulesFor(new DefaultBeanClass(AnnotatedController.class));
		Route route = getRouteMatching(routes, "/prefix/withoutPath");
		assertTrue(route.canHandle("/prefix/withoutPath"));
	}
	
	@Test
	public void shouldAddLocalePrefixAnnotatedControllerMethodWithoutPath() {
		List<Route> routes = parser.rulesFor(new DefaultBeanClass(AnnotatedController.class));
		Route route = getRouteMatching(routes, "/en-us/prefix/withoutPath");
		assertTrue(route.canHandle("/en-us/prefix/withoutPath"));
	}
	
	@Test
	public void shouldCanHandleAnnotatedControllerMethodAnnotatedWithoutPrefix() {
		List<Route> routes = parser.rulesFor(new DefaultBeanClass(AnnotatedController.class));
		Route route = getRouteMatching(routes, "/prefix/absolutePath");
		assertTrue(route.canHandle("/prefix/absolutePath"));
	}
	
	@Test
	public void shouldAddLocalePrefixAnnotatedControllerMethodAnnotated() {
		List<Route> routes = parser.rulesFor(new DefaultBeanClass(AnnotatedController.class));
		Route route = getRouteMatching(routes, "/en-us/prefix/absolutePath");
		assertTrue(route.canHandle("/en-us/prefix/absolutePath"));
	}
	
	@Test
	public void shouldCanHandleConventionControllerMethodWithoutPathWithoutPrefix() {
		List<Route> routes = parser.rulesFor(new DefaultBeanClass(ConventionController.class));
		Route route = getRouteMatching(routes, "/convention/withoutPath");
		assertTrue(route.canHandle("/convention/withoutPath"));
	}
	
	@Test
	public void shouldAddLocalePrefixConventionControllerMethodWithoutPath() {
		List<Route> routes = parser.rulesFor(new DefaultBeanClass(ConventionController.class));
		Route route = getRouteMatching(routes, "/en-us/convention/withoutPath");
		assertTrue(route.canHandle("/en-us/convention/withoutPath"));
	}
	
	@Test
	public void shouldCanHandleConventionControllerMethodAnnotatedWithoutPrefix() {
		List<Route> routes = parser.rulesFor(new DefaultBeanClass(ConventionController.class));
		Route route = getRouteMatching(routes, "/absolutePath");
		assertTrue(route.canHandle("/absolutePath"));
	}
	
	@Test
	public void shouldAddLocalePrefixConventionControllerMethodAnnotated() {
		List<Route> routes = parser.rulesFor(new DefaultBeanClass(ConventionController.class));
		Route route = getRouteMatching(routes, "/en-us/absolutePath");
		assertTrue(route.canHandle("/en-us/absolutePath"));
	}
	
	@Test
	public void shouldTranslateRouteToESAnnotatedControllerMethodWithoutPath() {
		List<Route> routes = parser.rulesFor(new DefaultBeanClass(AnnotatedController.class));
		Route route = getRouteMatching(routes, "/es-es/prefijo/withoutPath");
		assertNull(route);
	}
	
	@Test
	public void shouldTranslateRouteToESAnnotatedControllerMethodPath() {
		List<Route> routes = parser.rulesFor(new DefaultBeanClass(AnnotatedController.class));
		Route route = getRouteMatching(routes, "/es-es/prefijo/absoluto");
		assertNotNull(route);
		assertTrue(route.canHandle("/es-es/prefijo/absoluto"));
	}
	
	@Test
	public void shouldTranslateRouteToESSingleAnnotatedControllerMethodPath() {
		List<Route> routes = parser.rulesFor(new DefaultBeanClass(SingleAnnotatedController.class));
		Route route = getRouteMatching(routes, "/es-es/prefijo/absoluto");
		assertNotNull(route);
		assertTrue(route.canHandle("/es-es/prefijo/absoluto"));
	}
	
	@Test
	public void shouldTranslateRouteToESConventionControllerMethodWithoutPath() {
		List<Route> routes = parser.rulesFor(new DefaultBeanClass(ConventionController.class));
		Route route = getRouteMatching(routes, "/es-es/convention/withoutPath");
		assertNull(route);
	}
	
	@Test
	public void shouldTranslateRouteToPtBrConventionControllerMethodPath() {
		List<Route> routes = parser.rulesFor(new DefaultBeanClass(ConventionController.class));
		Route route = getRouteMatching(routes, "/es-es/absoluto");
		assertNotNull(route);
		assertTrue(route.canHandle("/es-es/absoluto"));
	}
	
	private Route getRouteMatching(List<Route> routes, String uri) {
		for (Route route : routes) {
			if (route.canHandle(uri)) {
				return route;
			}
		}
		return null;
	}

}
