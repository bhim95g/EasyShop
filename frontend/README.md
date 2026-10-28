# EasyShop browser storefront

This folder contains a static, dependency-free storefront UI that can be opened in a browser.

## Quick preview (no backend required)

From the repository root:

```bash
cd frontend
python3 -m http.server 5500
```

Open **http://localhost:5500**. The app starts in **Demo (no server)** mode: browse items, search, filter by category, add/update items in the cart, and edit a sample profile. Demo changes are in-memory and reset when the page reloads.

## Connect the UI to EasyShop

1. Install Java 17, Maven, and MySQL. Create/populate the EasyShop database using `capstone-starter/database/create_database.sql`.
2. Configure the backend's datasource and JWT settings securely (do not commit real credentials or secrets).
3. From `capstone-starter/`, start the backend using `mvn spring-boot:run` (or `./mvnw spring-boot:run`). Default base URL: **http://localhost:8080**.
4. Serve the frontend as above. Switch **Connection** to **Live API**, confirm the backend URL, and click **Refresh**.
5. Catalog and category data now load from `GET /products` and `GET /categories`.
6. Choose **My profile** or **Add** to open the login panel. Sign in with an existing backend account using `POST /login`. Once signed in, the app can read/edit the profile and manage the persisted shopping cart.

**Important:** The profile endpoints **GET /profile** and **PUT /profile** are proposed in [PR #3](https://github.com/bhim95g/EasyShop/pull/3), not currently present in `main`. Run the backend from `feature/customer-profile-api` or merge PR #3 before using the profile screen in Live API mode.

The cart error handling and validation improvements are separately proposed in [PR #2](https://github.com/bhim95g/EasyShop/pull/2). Without them, failed cart operations can return confusing statuses.

## Scope and limitations

- **Demo** uses sample products and an in-memory cart/profile; it does not test database persistence or authentication.
- **Live API** uses the repository's actual `/products`, `/categories`, `/login`, `/cart` and `/profile` endpoints. JWT is held in page memory only and cleared on reload/mode switch. Never share or enter real credentials into someone else's deployed preview.
- Cart APIs have no remove-single-item endpoint exposed by the current controller. Use **Clear cart** to clear the entire cart. Checkout/payment/ordering APIs are not yet implemented.
- Product photos in the sample DB use relative filenames with no included image assets; the UI displays category/product emoji illustrations as placeholders instead.
- The backend uses CORS annotations. If you use a different origin, check Spring's CORS settings and browser console.
- The existing Maven configuration has dependency mismatches and older versions; resolve build failures before relying on deployment. In particular the project pins Spring JDBC 6 while using Spring Boot 2.7.
- **Security:** The current registration implementation appears to trust a client-provided role, and the repository has committed datasource/JWT config values. Do not publish the backend to the open internet until those issues are remediated.

This PR only introduces the static frontend and does not change `main` or deploy a site.

## Checkout + test payments (new)

The UI now has **Proceed to checkout** in the cart and **Orders** in the header. In **Demo** mode you can place an in-memory order and simulate an approval or decline without a backend. These demo orders reset on page reload.

To save orders to MySQL and test real HTTP endpoints in **Live API** mode, use this branch for the backend as well, apply `capstone-starter/database/checkout_migration.sql` once, and follow the detailed instructions in [capstone-starter/CHECKOUT.md](../capstone-starter/CHECKOUT.md).

Set `CHECKOUT_TEST_PAYMENTS_ENABLED=true` on your local backend to enable the simulated payment controls. For the optional **Stripe-hosted test Checkout**, set a Stripe `sk_test_` secret and run a signed webhook forwarder as explained in CHECKOUT.md. This does **not** support actual money or credit-card collection by EasyShop.

On a Stripe redirect back to this frontend, open **Orders** after the backend receives its signed webhook. A browser success redirect alone is not confirmation of payment. Pending Stripe orders reserve inventory until a signed payment/expiry event is processed; production would need resilient reconciliation.
