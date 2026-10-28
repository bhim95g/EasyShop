package org.yearup.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.data.ProductDao;
import org.yearup.data.ShoppingCartDao;
import org.yearup.data.UserDao;
import org.yearup.models.Product;
import org.yearup.models.ShoppingCart;
import org.yearup.models.ShoppingCartItem;
import org.yearup.models.User;

import java.security.Principal;

@RestController
@RequestMapping("/cart")
@CrossOrigin
@PreAuthorize("isAuthenticated()")
public class ShoppingCartController
{
    private final ShoppingCartDao shoppingCartDao;
    private final UserDao userDao;
    private final ProductDao productDao;

    @Autowired
    public ShoppingCartController(ShoppingCartDao shoppingCartDao, UserDao userDao, ProductDao productDao)
    {
        this.shoppingCartDao = shoppingCartDao;
        this.userDao = userDao;
        this.productDao = productDao;
    }

    @GetMapping
    public ShoppingCart getCart(Principal principal)
    {
        try
        {
            return shoppingCartDao.getByUserId(getUserId(principal));
        }
        catch (ResponseStatusException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load cart.", e);
        }
    }

    @PostMapping("/products/{productId}")
    public void addToCart(@PathVariable int productId, Principal principal)
    {
        try
        {
            int userId = getUserId(principal);
            Product product = productDao.getById(productId);
            if (product == null)
            {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found.");
            }

            ShoppingCartItem item = new ShoppingCartItem();
            item.setProduct(product);
            item.setQuantity(1);
            shoppingCartDao.addToCart(userId, item);
        }
        catch (ResponseStatusException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to add to cart.", e);
        }
    }

    @PutMapping("/products/{productId}")
    public void updateQuantity(@PathVariable int productId,
                               @RequestBody ShoppingCartItem body,
                               Principal principal)
    {
        if (body == null || body.getQuantity() < 1)
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be at least 1.");
        }

        try
        {
            int updatedRows = shoppingCartDao.updateQuantity(getUserId(principal), productId, body.getQuantity());
            if (updatedRows == 0)
            {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found.");
            }
        }
        catch (ResponseStatusException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update cart item.", e);
        }
    }

    @DeleteMapping
    public void clearCart(Principal principal)
    {
        try
        {
            shoppingCartDao.clearCart(getUserId(principal));
        }
        catch (ResponseStatusException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to clear cart.", e);
        }
    }

    private int getUserId(Principal principal)
    {
        if (principal == null)
        {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }

        User user = userDao.getByUserName(principal.getName());
        if (user == null)
        {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found.");
        }
        return user.getId();
    }
}
