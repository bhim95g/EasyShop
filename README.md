# 🛍️ EasyShop – E-Commerce Spring Boot API

EasyShop is a Java Spring Boot REST API that powers the backend of an online store. It handles products, categories, user profiles, shopping carts and checkout, with JWT-based login and admin/user roles.

---

## 🚀 Features

- Register and log in with a JWT token
- Role-based access control (admin vs. user)
- Browse and search products by category, price range and color
- Full CRUD for products and categories (admin only)
- View and update your own profile, including your shipping address
- Shopping cart: add products, change quantities, clear the cart
- Checkout: turn your cart into an order and view your order history
- MySQL database

---

## 🧰 Tech Stack

- Java 17, Spring Boot 2.7
- Spring Security with JWT (jjwt)
- Spring JDBC with MySQL
- Maven

---

## ⚙️ Getting Started

1. Create the database by running `capstone-starter/database/create_database.sql` in MySQL, for example `mysql -u root -p < capstone-starter/database/create_database.sql`. It loads 200 products in 5 categories and 3 sample users. Running it again rebuilds the `easyshop` database from scratch, which deletes any accounts and orders you created.
2. Set your MySQL connection details in `capstone-starter/src/main/resources/application.properties` (`datasource.url`, `datasource.username`, `datasource.password`).
3. Start the API:
   ```bash
   cd capstone-starter
   ./mvnw spring-boot:run
   ```
4. The API runs at `http://localhost:8080`. Log in with `POST /login`, then send the token as `Authorization: Bearer <token>` on protected requests.

---

## 📡 API Endpoints

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/register` | Public | Create an account (an empty profile is created too) |
| POST | `/login` | Public | Log in and receive a JWT |
| GET | `/categories` | Public | List all categories |
| GET | `/categories/{id}` | Public | Get one category |
| GET | `/categories/{id}/products` | Public | Products in a category |
| POST | `/categories` | Admin | Add a category |
| PUT | `/categories/{id}` | Admin | Update a category |
| DELETE | `/categories/{id}` | Admin | Delete a category |
| GET | `/products?categoryId=&minPrice=&maxPrice=&color=` | Public | Search products (all filters optional) |
| GET | `/products/{id}` | Public | Get one product |
| GET | `/products/category/{categoryId}` | Public | Products in a category |
| POST | `/products` | Admin | Add a product |
| PUT | `/products/{id}` | Admin | Update a product |
| DELETE | `/products/{id}` | Admin | Delete a product |
| GET | `/profile` | Logged in | Get your profile |
| PUT | `/profile` | Logged in | Update your profile and shipping address |
| GET | `/cart` | Logged in | View your cart and its total |
| POST | `/cart/products/{productId}` | Logged in | Add a product (adds 1 if already in the cart) |
| PUT | `/cart/products/{productId}` | Logged in | Set a product's quantity, body: `{ "quantity": 3 }` |
| DELETE | `/cart` | Logged in | Empty your cart |
| POST | `/orders` | Logged in | Check out: create an order from your cart, then empty it |
| GET | `/orders` | Logged in | Your order history, newest first |

### Checkout flow

1. Fill in your shipping address with `PUT /profile`.
2. Add products with `POST /cart/products/{productId}`.
3. Check out with `POST /orders` (no body). The order ships to your profile address. The order, its line items and emptying the cart are saved in a single database transaction, so a failure part-way leaves nothing half-done.

Checkout returns `400 Bad Request` if your cart is empty or your profile has no shipping address.

---

## 🛍️ Storefront Website

A single-page storefront lives in `frontend/index.html`. It talks to the API above, or runs on built-in demo data with no server.

**Try it:**
1. Start MySQL and run the Spring Boot app (see *Getting Started*).
2. Open `frontend/index.html` in your browser.
3. Scroll to the footer, click **Developer settings**, and set **Connection** to **Live API** (`http://localhost:8080`). The tag next to it changes to **Live API** and the shop lists all 200 products from the database. The choice is remembered in your browser.
4. Click **Join now** to create an account, add items to your bag, and choose **Place order**. Your orders appear under **My orders** (the box icon in the header).

With **Demo (no server)** you can click around with sample data and no backend. If the site can't reach the API, it switches to demo data and tells you.

**Features:**
- 200 products in 5 categories, each with a product photo and a lifestyle photo
- Search panel with popular and recent searches (press `/` to open it)
- Big menu with product groups for each category, and a ☰ menu on phones
- Filter sidebar: category, group, price, color, in stock only and new arrivals
- NEW badges and a New arrivals row
- Product pages with 4 photo views, a size guide and a sticky **Add to bag** bar on phones
- Quick view, recently viewed, and sorting by price, name or newest
- Shopping bag, checkout, order history and profile editing
- Video hero, the Journal (3 stories), help pages, and a newsletter sign-up (saved in the browser only; no emails are sent)

Product photos and the hero video are free stock media from [Pexels](https://www.pexels.com) and load from the internet.

---

## 🧠 Interesting Code Snippet

Checkout saves the order, its line items and the emptied cart as one transaction: either everything is saved or nothing is.

```java
connection.setAutoCommit(false);
try
{
    // insert the order, then one line item per cart item ...
    // ... then empty the user's cart
    connection.commit();
    return order;
}
catch (SQLException e)
{
    connection.rollback();
    throw e;
}
```

---

## 📸 Screenshots

_Add Postman or app screenshots to a `screenshots/` folder and link them here, for example:_
`![Categories test](screenshots/categories-test.png)`
