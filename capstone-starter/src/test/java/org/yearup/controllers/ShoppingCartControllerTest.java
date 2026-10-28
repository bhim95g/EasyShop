package org.yearup.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.data.ProductDao;
import org.yearup.data.ShoppingCartDao;
import org.yearup.data.UserDao;
import org.yearup.models.Product;
import org.yearup.models.ShoppingCartItem;
import org.yearup.models.User;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class ShoppingCartControllerTest
{
    private ShoppingCartDao shoppingCartDao;
    private UserDao userDao;
    private ProductDao productDao;
    private ShoppingCartController controller;
    private final Principal principal = () -> "alice";

    @BeforeEach
    void setUp()
    {
        shoppingCartDao = mock(ShoppingCartDao.class);
        userDao = mock(UserDao.class);
        productDao = mock(ProductDao.class);
        controller = new ShoppingCartController(shoppingCartDao, userDao, productDao);

        User user = new User();
        user.setId(7);
        when(userDao.getByUserName("alice")).thenReturn(user);
    }

    @Test
    void addToCart_returns404ForMissingProduct()
    {
        when(productDao.getById(99)).thenReturn(null);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class, () -> controller.addToCart(99, principal));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
        verify(shoppingCartDao, never()).addToCart(anyInt(), any());
    }

    @Test
    void addToCart_addsOneItemForAuthenticatedUser()
    {
        Product product = new Product();
        product.setProductId(15);
        when(productDao.getById(15)).thenReturn(product);

        controller.addToCart(15, principal);

        verify(shoppingCartDao).addToCart(eq(7), argThat(item ->
                item.getProduct() == product && item.getQuantity() == 1));
    }

    @Test
    void addToCart_returns500WhenDatabaseOperationFails()
    {
        Product product = new Product();
        product.setProductId(15);
        when(productDao.getById(15)).thenReturn(product);
        IllegalStateException databaseFailure = new IllegalStateException("Database unavailable");
        doThrow(databaseFailure).when(shoppingCartDao).addToCart(eq(7), any());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class, () -> controller.addToCart(15, principal));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, error.getStatus());
        assertSame(databaseFailure, error.getCause());
    }

    @Test
    void updateQuantity_returns400ForZeroOrNegativeQuantity()
    {
        for (int quantity : new int[] {0, -1})
        {
            ShoppingCartItem body = new ShoppingCartItem();
            body.setQuantity(quantity);

            ResponseStatusException error = assertThrows(
                    ResponseStatusException.class, () -> controller.updateQuantity(15, body, principal));

            assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        }

        verifyNoInteractions(shoppingCartDao);
    }

    @Test
    void updateQuantity_returns404WhenItemIsNotInCart()
    {
        ShoppingCartItem body = new ShoppingCartItem();
        body.setQuantity(2);
        when(shoppingCartDao.updateQuantity(7, 15, 2)).thenReturn(false);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class, () -> controller.updateQuantity(15, body, principal));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
    }

    @Test
    void updateQuantity_acceptsExistingItem()
    {
        ShoppingCartItem body = new ShoppingCartItem();
        body.setQuantity(2);
        when(shoppingCartDao.updateQuantity(7, 15, 2)).thenReturn(true);

        controller.updateQuantity(15, body, principal);

        verify(shoppingCartDao).updateQuantity(7, 15, 2);
    }

    @Test
    void clearCart_requiresAnExistingAuthenticatedUser()
    {
        when(userDao.getByUserName("alice")).thenReturn(null);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class, () -> controller.clearCart(principal));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatus());
        verify(shoppingCartDao, never()).clearCart(anyInt());
    }

    @Test
    void getCart_returns500WhenDatabaseOperationFails()
    {
        IllegalStateException databaseFailure = new IllegalStateException("Database unavailable");
        when(shoppingCartDao.getByUserId(7)).thenThrow(databaseFailure);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class, () -> controller.getCart(principal));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, error.getStatus());
        assertSame(databaseFailure, error.getCause());
    }
}
