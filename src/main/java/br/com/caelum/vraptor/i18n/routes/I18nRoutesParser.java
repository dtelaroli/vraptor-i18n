package br.com.caelum.vraptor.i18n.routes;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.interceptor.Interceptor;

import br.com.caelum.vraptor.Path;
import br.com.caelum.vraptor.controller.BeanClass;
import br.com.caelum.vraptor.controller.HttpMethod;
import br.com.caelum.vraptor.http.route.PathAnnotationRoutesParser;
import br.com.caelum.vraptor.http.route.Route;
import br.com.caelum.vraptor.http.route.RouteBuilder;
import br.com.caelum.vraptor.http.route.Router;

@ApplicationScoped
@Alternative
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class I18nRoutesParser extends PathAnnotationRoutesParser {

	private final List<Route> routes;
	private final RoutesResources routesResource;
	private final Router router;
	
	/** 
	 * @deprecated CDI eyes only
	 */
	protected I18nRoutesParser() {
		this(null, null);
	}

	@Inject
	public I18nRoutesParser(Router router, RoutesResources routesResource) {
		super(router);
		this.router = router;
		this.routesResource = routesResource;
		routes = new ArrayList<>();
	}
	
	@Override
	public List<Route> rulesFor(BeanClass controller) {
		routes.addAll(super.rulesFor(controller));
		return routes;
	}
	
	@Override
	protected String[] getURIsFor(Method javaMethod, Class<?> type) {
		String[] urIsFor = super.getURIsFor(javaMethod, type);
		
		for (int i = 0; i < urIsFor.length; i++) {
			for (ResourceBundle bundle : routesResource.getAvailableBundles()) {
				if(bundle.containsKey(urIsFor[i])) {
					Route translatedRouteWithLocalePrefix = buildWithLocalePrefix(javaMethod, type, bundle.getString(urIsFor[i]), getLocalePrefix(bundle.getLocale()), getPriority(javaMethod));
					routes.add(translatedRouteWithLocalePrefix);
				}
			}
			
			Route defaultRouteWithLocalePrefix = buildWithLocalePrefix(javaMethod, type, urIsFor[i], getLocalePrefix(Locale.getDefault()), getPriority(javaMethod));
			routes.add(defaultRouteWithLocalePrefix);
		}
		
		bildLocalizedURIsFor(javaMethod, type);
		
		return urIsFor;
	}
	
	protected void bildLocalizedURIsFor(Method javaMethod, Class<?> type) {
		if (javaMethod.isAnnotationPresent(I18nPaths.class)) {
			I18nPath[] i18nPaths = javaMethod.getAnnotation(I18nPaths.class).value();
			for (I18nPath i18nPath : i18nPaths) {
				buildLocalizedURI(i18nPath, javaMethod, type);
			}
		} 
		else if(javaMethod.isAnnotationPresent(I18nPath.class)) {
			buildLocalizedURI(javaMethod.getAnnotation(I18nPath.class), javaMethod, type);
		}
	}
	

	private void buildLocalizedURI(I18nPath i18nPath, Method javaMethod, Class<?> type) {
		String locale = i18nPath.locale();
		String value = fixLeadingSlash(i18nPath.path());

		I18nPath i18nPathType = findByLocale(locale, type);
		if(i18nPathType != null) {
			value = fixLeadingSlash(i18nPathType.path()) + value;
		}
		
		Route buildWithLocalePrefix = buildWithLocalePrefix(javaMethod, type, value, "/" + locale, Path.DEFAULT);
		routes.add(buildWithLocalePrefix);
	}
	
	private I18nPath findByLocale(String locale, Class<?> type) {
		if(type.isAnnotationPresent(I18nPath.class)) {
			return type.getAnnotation(I18nPath.class);
		}
		else if(type.isAnnotationPresent(I18nPaths.class)) {
			I18nPath[] i18nPaths = type.getAnnotation(I18nPaths.class).value();
			for (I18nPath i18nPath : i18nPaths) {
				if(i18nPath.locale().equals(locale)) {
					return i18nPath;
				}
			}
		}
		return null;
	}

	private String fixLeadingSlash(String uri) {
		if (!uri.startsWith("/")) {
			return  "/" + uri;
		}
		return uri;
	}
	
	private Route buildWithLocalePrefix(Method javaMethod, Class<?> type, String uri, String localePrefix, int priority) {
		RouteBuilder rule = router.builderFor(localePrefix + uri);

		EnumSet<HttpMethod> methods = getHttpMethods(javaMethod);
		EnumSet<HttpMethod> typeMethods = getHttpMethods(type);

		rule.with(methods.isEmpty() ? typeMethods : methods)
			.withPriority(priority)
			.is(type, javaMethod);

		return rule.build();
	}

	private String getLocalePrefix(Locale locale) {
		String localePrefix = "/" + locale.getLanguage();
		
		if(!locale.getCountry().isEmpty()) {
			 localePrefix += "-" + locale.getCountry().toLowerCase();
		}
		return localePrefix;
	}
	
	private int getPriority(Method javaMethod) {
		if(javaMethod.isAnnotationPresent(Path.class)){
			return javaMethod.getAnnotation(Path.class).priority();
		}

		return Path.DEFAULT;
	}
	
	private EnumSet<HttpMethod> getHttpMethods(AnnotatedElement annotated) {
		EnumSet<HttpMethod> methods = EnumSet.noneOf(HttpMethod.class);
		for (HttpMethod method : HttpMethod.values()) {
			if (annotated.isAnnotationPresent(method.getAnnotation())) {
				methods.add(method);
			}
		}
		return methods;
	}

}