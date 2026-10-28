# Checkout and payment testing

**Status: developer/test environment only. No live payments or production fulfilment.**

This feature adds orders, shipping-address capture, reserved inventory, an authenticated order history API, simulated approved/declined payments, and optional **Stripe-hosted Checkout in test mode**. Real cards and live Stripe keys are intentionally rejected.

## 1. Database migration

Use the original `database/create_database.sql` **only for a fresh demo database**; it starts with `DROP DATABASE IF EXISTS easyshop`.

For an existing EasyShop database, preserve the data and apply only:

```bash
mysql -u root -p < database/checkout_migration.sql
```

The additive migration extends the **existing** `orders` and `order_line_items` tables, sets existing orders to `LEGACY`, adds product-name snapshots, and indexes orders/payment references. It is a **one-time** migration.

## 2. Start simulated payments

Configure a local MySQL connection and the existing JWT secret in your backend configuration. **Do not expose the backend publicly**: existing committed development credentials and seed accounts must be remediated.

```bash
cd capstone-starter
export CHECKOUT_TEST_PAYMENTS_ENABLED=true
./mvnw spring-boot:run
```

Open the frontend on another terminal:

```bash
cd frontend
python3 -m http.server 5500
```

Browse to `http://localhost:5500`; switch the frontend to **Live API**, set `http://localhost:8080`, log in, add products, and proceed through the shipping form. Choose **Simulated test payment** and either **Approve** or **Decline**.

Simulated-payment endpoints are disabled by default. The client cannot set the order total, user ID, or stock. Server-side checkout reads the DB cart and product prices, validates and reserves stock within one transaction, creates an order snapshot, and clears the cart. A declined payment restores the reserved stock; a paid order keeps the reservation. The user can view past orders without seeing other users' orders.

## 3. Stripe hosted Checkout — test mode only

Create a Stripe account, enable test mode, and obtain your **test secret key** (`sk_test_...`) from your Stripe dashboard. Do not send it through chat or commit it.

Install and authenticate the [Stripe CLI](https://docs.stripe.com/stripe-cli), then forward webhook events:

```bash
stripe listen --forward-to localhost:8080/checkout/stripe/webhook
```

The CLI prints a webhook signing secret (`whsec_...`). Configure both values in the backend environment before launching Java:

```bash
export CHECKOUT_TEST_PAYMENTS_ENABLED=true
export CHECKOUT_STRIPE_TEST_SECRET_KEY='sk_test_YOUR_TEST_KEY'
export CHECKOUT_STRIPE_WEBHOOK_SECRET='whsec_YOUR_CLI_WEBHOOK_SECRET'
export CHECKOUT_FRONTEND_URL='http://localhost:5500'
./mvnw spring-boot:run
```

With **Live API** mode enabled in the frontend, choose **Stripe hosted test checkout** at checkout. The browser navigates to Stripe's HTTPS-hosted Checkout page; card details are never collected by EasyShop. Use Stripe's [documented test cards](https://docs.stripe.com/testing) to exercise successful and declined transactions; real cards must never be used for testing.

**Important:** the browser return URL is *not* payment confirmation. Only a correctly signed `checkout.session.completed` / `checkout.session.async_payment_succeeded` event with `payment_status=paid`, the correct test session, correct currency and matching amount can mark an order paid. Signed `checkout.session.expired` or asynchronous failures release the stock reservation. A user who simply closes the Stripe page has a `PENDING_PAYMENT` order until the signed expiry event arrives.

## API

| Method | Endpoint | Authentication | Meaning |
| --- | --- | --- | --- |
| `GET` | `/checkout/config` | Bearer JWT | Sandbox payment availability |
| `POST` | `/checkout/orders` | Bearer JWT | Create order from DB cart/shipping payload |
| `GET` | `/checkout/orders` | Bearer JWT | View own last 100 orders |
| `GET` | `/checkout/orders/{id}` | Bearer JWT | View own order details |
| `POST` | `/checkout/orders/{id}/simulate` | Bearer JWT + simulator enabled | Approve/decline own SIMULATED order |
| `POST` | `/checkout/stripe/webhook` | Stripe signature, not JWT | Trusted Stripe event receiver |

Create-order example (the client **never** submits prices, quantities, or an arbitrary user ID):

```json
{
  "address": "123 Main Street",
  "city": "Pittsburgh",
  "state": "PA",
  "zip": "15222",
  "paymentMethod": "SIMULATED"
}
```

For the simulator, POST `{"outcome":"APPROVED"}` or `{"outcome":"DECLINED"}` to the test-payment route. Stripe test sessions return `checkoutUrl` for a one-time redirect.

## Running automated tests

```bash
cd capstone-starter
./mvnw -Dtest=CheckoutControllerTest,StripeTestWebhookControllerTest test
```

The checkout GitHub Actions workflow runs those isolated tests. **Full MySQL integration tests and real Stripe test-mode webhook E2E validation still require a local database and Stripe test account.**

## Production limitations — do not deploy as a real shop

- No **live payment processing**; only simulated payments and Stripe `sk_test_` keys are accepted.
- Shipping is collected, but rates, taxes, discounts, refunds, fulfillment, email receipts, and shipping providers have not been implemented; shipping is presently $0.
- Stock is reserved when a pending order is placed. **If Stripe webhook delivery fails**, reservation can remain stuck; production would require a durable reconciliation/expiry worker, alerts, and recovery/refund handling.
- Real payment operations need idempotent fulfilment, PCI/privacy reviews, abuse protection, stronger customer authentication and proper monitoring.
- The repository contains committed datasource/JWT values and development accounts. Rotate any real credentials and scrub Git history where appropriate before production.
- The existing feature PR for `GET/PUT /profile` is separate from checkout. Checkout only requires existing user authentication and cart endpoints.
