package org.yearup.services.checkout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Server-side Stripe Checkout sessions, explicitly restricted to test keys. */
@Service
public class StripeTestCheckoutClient
{
    private final String secretKey;
    private final String frontendUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public StripeTestCheckoutClient(
            @Value("${checkout.stripe.test-secret-key:}") String secretKey,
            @Value("${checkout.frontend-url:http://localhost:5500}") String frontendUrl,
            ObjectMapper objectMapper)
    {
        this.secretKey = secretKey;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public boolean isConfigured()
    {
        return secretKey.startsWith("sk_test_") && secretKey.length() > 12;
    }

    public StripeSession createSession(Map<String, Object> order)
    {
        if (!isConfigured())
            throw new IllegalStateException("Stripe test secret key is not configured.");
        long orderId = ((Number) order.get("orderId")).longValue();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("Empty order.");

        List<String> params = new ArrayList<>();
        add(params, "mode", "payment");
        add(params, "payment_method_types[0]", "card");
        add(params, "client_reference_id", Long.toString(orderId));
        add(params, "metadata[order_id]", Long.toString(orderId));
        add(params, "success_url", frontendUrl + "/?checkout=success&order_id=" + orderId);
        add(params, "cancel_url", frontendUrl + "/?checkout=cancel&order_id=" + orderId);
        add(params, "expires_at", Long.toString(Instant.now().getEpochSecond() + 1800));

        for (int i = 0; i < items.size(); i++)
        {
            Map<String, Object> line = items.get(i);
            String prefix = "line_items[" + i + "]";
            BigDecimal price = (BigDecimal) line.get("unitPrice");
            long cents = price.movePointRight(2).longValueExact();
            if (cents < 1)
                throw new IllegalArgumentException("Stripe Checkout requires positive unit prices.");
            add(params, prefix + "[price_data][currency]", "usd");
            add(params, prefix + "[price_data][unit_amount]", Long.toString(cents));
            add(params, prefix + "[price_data][product_data][name]", String.valueOf(line.get("name")));
            add(params, prefix + "[quantity]", String.valueOf(line.get("quantity")));
        }

        String body = String.join("&", params);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.stripe.com/v1/checkout/sessions"))
                .header("Authorization", "Bearer " + secretKey)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Idempotency-Key", "easyshop-test-order-" + orderId)
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try
        {
            HttpResponse<String> result = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (result.statusCode() != 200)
                throw new IllegalStateException("Stripe test Checkout rejected the session request (HTTP " + result.statusCode() + ").");
            JsonNode response = objectMapper.readTree(result.body());
            if (!response.path("id").asText("").startsWith("cs_test_"))
                throw new IllegalStateException("Stripe returned a non-test session.");
            String url = response.path("url").asText("");
            if (!url.startsWith("https://checkout.stripe.com/"))
                throw new IllegalStateException("Stripe returned an invalid checkout URL.");
            return new StripeSession(response.path("id").asText(), url);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Stripe request interrupted.", e);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Stripe test checkout session could not be created.", e);
        }
    }

    private static void add(List<String> params, String key, String value)
    {
        params.add(URLEncoder.encode(key, StandardCharsets.UTF_8) + "=" +
                URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    public static final class StripeSession
    {
        public final String id;
        public final String url;
        StripeSession(String id, String url)
        {
            this.id = id;
            this.url = url;
        }
    }
}
