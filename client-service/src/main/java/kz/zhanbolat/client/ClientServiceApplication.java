package kz.zhanbolat.client;

import brave.Tracer;
import brave.Tracing;
import brave.http.HttpRequestParser;
import brave.http.HttpSampler;
import brave.http.HttpTags;
import brave.http.HttpTracing;
import brave.okhttp3.TracingCallFactory;
import brave.okhttp3.TracingInterceptor;
import brave.sampler.Sampler;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.okhttp3.OkHttpSender;

@SpringBootApplication
public class ClientServiceApplication implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(ClientServiceApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(ClientServiceApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

		var sender = OkHttpSender.create("http://127.0.0.1:9411/api/v2/spans");

		var spanHandler = AsyncZipkinSpanHandler.create(sender);
		Tracing tracing = Tracing.newBuilder().localServiceName("client-service")
				.addSpanHandler(spanHandler)
				.build();

		HttpTracing httpTracing = HttpTracing.create(tracing).toBuilder()
				.clientRequestParser((request, context, span) -> {
					HttpRequestParser.DEFAULT.parse(request, context, span);
					HttpTags.URL.tag(request, context, span);
				}).build();

		OkHttpClient client = new OkHttpClient.Builder().build();

		Call.Factory callFactory = TracingCallFactory.create(httpTracing, client);

		Request request = new Request.Builder().url("http://localhost:8080/api/message").build();
		try (Response response = callFactory.newCall(request).execute()) {
			logger.info("Response:\nstatus: {}\nheaders: {}\nbody: {}", response.code(), response.headers(), response.body().string());
		} finally {
			httpTracing.close();
			tracing.close();
			spanHandler.flush();
			spanHandler.close();
			sender.close();
		}
	}
}
