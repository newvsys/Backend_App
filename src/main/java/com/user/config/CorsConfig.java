package com.user.config;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

@Configuration
@EnableTransactionManagement
public class CorsConfig {

	@Value("${razorpay.key.id}")
	private String keyId;

	@Value("${razorpay.key.secret}")
	private String keySecret;

	@Bean
	public RazorpayClient razorpayClient() throws RazorpayException {
		return new RazorpayClient(keyId, keySecret);
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Value("${cors.allowed-origin-patterns}")
	private String allowedOriginPatterns;

	@Value("${label.pdf.dir:/public/labels/}")
	private String labelPdfDir;

	@Bean
	public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowCredentials(true);

		List<String> origins = Arrays.stream(allowedOriginPatterns.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.toList();
		origins.forEach(config::addAllowedOriginPattern);

		config.addAllowedHeader("*");
		config.addAllowedMethod("*");
		config.setExposedHeaders(List.of("Authorization", "Content-Disposition", "X-Total-Count"));
		config.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);

		FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
		bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
		// Also apply CORS headers on error-dispatched responses so the browser
		// sees the real HTTP error (404, 500…) instead of a fake CORS error.
		bean.setDispatcherTypes(EnumSet.of(
				DispatcherType.REQUEST,
				DispatcherType.ASYNC,
				DispatcherType.ERROR
		));
		return bean;
	}

	@Bean
	public WebMvcConfigurer webMvcConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addResourceHandlers(ResourceHandlerRegistry registry) {
				String dir = labelPdfDir.endsWith("/") ? labelPdfDir : labelPdfDir + "/";
				registry.addResourceHandler("/labels/**").addResourceLocations("file:" + dir);
			}
		};
	}


}
