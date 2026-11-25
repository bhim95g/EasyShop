package org.yearup.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.data.OrderDao;
import org.yearup.data.ProfileDao;
import org.yearup.data.ShoppingCartDao;
import org.yearup.data.UserDao;
import org.yearup.models.Order;
import org.yearup.models.Profile;
import org.yearup.models.ShoppingCart;
import org.yearup.models.User;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/orders")
@CrossOrigin
@PreAuthorize("isAuthenticated()") // only logged in users can check out or see their orders
public class OrdersController
{
    private final OrderDao orderDao;
    private final ShoppingCartDao shoppingCartDao;
    private final ProfileDao profileDao;
    private final UserDao userDao;

    @Autowired
    public OrdersController(OrderDao orderDao, ShoppingCartDao shoppingCartDao, ProfileDao profileDao, UserDao userDao)
    {
        this.orderDao = orderDao;
        this.shoppingCartDao = shoppingCartDao;
        this.profileDao = profileDao;
        this.userDao = userDao;
    }

    // POST http://localhost:8080/orders  (no body)
    // checks out the current user's cart: creates the order, ships to the profile address, empties the cart
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order checkout(Principal principal)
    {
        int userId = currentUserId(principal);

        ShoppingCart cart = shoppingCartDao.getByUserId(userId);
        if (cart.getItems().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your cart is empty.");

        Profile profile = profileDao.getByUserId(userId);
        if (profile == null || isBlank(profile.getAddress()) || isBlank(profile.getCity())
                || isBlank(profile.getState()) || isBlank(profile.getZip()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Add a shipping address to your profile (PUT /profile) before checking out.");

        try
        {
            return orderDao.checkout(userId, profile, cart);
        }
        catch (Exception e)
        {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Checkout failed.");
        }
    }

    // GET http://localhost:8080/orders  - the current user's order history, newest first
    @GetMapping
    public List<Order> getOrders(Principal principal)
    {
        int userId = currentUserId(principal);

        try
        {
            return orderDao.getByUserId(userId);
        }
        catch (Exception e)
        {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load orders.");
        }
    }

    private int currentUserId(Principal principal)
    {
        User user = userDao.getByUserName(principal.getName());
        if (user == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found.");
        return user.getId();
    }

    private static boolean isBlank(String value)
    {
        return value == null || value.trim().isEmpty();
    }
}
