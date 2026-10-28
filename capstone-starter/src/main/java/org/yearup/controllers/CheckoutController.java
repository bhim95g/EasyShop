package org.yearup.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.data.UserDao;
import org.yearup.data.mysql.CheckoutOrderDao;
import org.yearup.models.User;
import org.yearup.models.checkout.CheckoutRequest;
import org.yearup.services.checkout.StripeTestCheckoutClient;

import javax.validation.Valid;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/checkout")
@CrossOrigin
@PreAuthorize("isAuthenticated()")
public class CheckoutController
{
    private final CheckoutOrderDao orders;
    private final UserDao users;
    private final StripeTestCheckoutClient stripe;
    private final boolean simulatorEnabled;

    public CheckoutController(CheckoutOrderDao orders, UserDao users, StripeTestCheckoutClient stripe,
                              @Value("${checkout.test-payments-enabled:false}") boolean simulatorEnabled)
    {
        this.orders = orders;
        this.users = users;
        this.stripe = stripe;
        this.simulatorEnabled = simulatorEnabled;
    }

    @GetMapping("/config")
    public Map<String, Object> config(Principal principal)
    {
        userId(principal);
        return Map.of("simulatedPaymentsEnabled", simulatorEnabled,
                      "stripeTestEnabled", stripe.isConfigured(),
                      "livePaymentsEnabled", false);
    }

    @GetMapping("/orders")
    public List<Map<String, Object>> listOrders(Principal principal)
    {
        return orders.getOrders(userId(principal));
    }

    @GetMapping("/orders/{id}")
    public Map<String, Object> getOrder(@PathVariable long id, Principal principal)
    {
        return orders.getOrder(userId(principal), id);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createOrder(@Valid @RequestBody CheckoutRequest request, Principal principal)
    {
        int customerId = userId(principal);
        if ("SIMULATED".equals(request.getPaymentMethod()) && !simulatorEnabled)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Simulated payments are disabled.");
        if ("STRIPE_TEST".equals(request.getPaymentMethod()) && !stripe.isConfigured())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe test payment is not configured.");

        long id = orders.createOrder(customerId, request);
        Map<String, Object> response = new LinkedHashMap<>(orders.getOrder(customerId, id));
        if ("STRIPE_TEST".equals(request.getPaymentMethod()))
        {
            try
            {
                StripeTestCheckoutClient.StripeSession session = stripe.createSession(response);
                orders.attachStripeSession(id, session.id);
                response.put("checkoutUrl", session.url);
            }
            catch (RuntimeException e)
            {
                // Inventory must not remain reserved if no payment session could be created.
                orders.failStripeSession(id);
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe test checkout is unavailable.", e);
            }
        }
        return response;
    }

    @PostMapping("/orders/{id}/simulate")
    public Map<String, Object> simulate(@PathVariable long id,
                                         @RequestBody Map<String, String> payload,
                                         Principal principal)
    {
        int customerId = userId(principal);
        if (!simulatorEnabled)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Simulated payments are disabled.");
        String outcome = payload.get("outcome");
        if (!"APPROVED".equals(outcome) && !"DECLINED".equals(outcome))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Outcome must be APPROVED or DECLINED.");
        orders.simulatePayment(customerId, id, "APPROVED".equals(outcome));
        return orders.getOrder(customerId, id);
    }

    private int userId(Principal principal)
    {
        if (principal == null || principal.getName() == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in required.");
        User customer = users.getByUserName(principal.getName());
        if (customer == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found.");
        return customer.getId();
    }
}
