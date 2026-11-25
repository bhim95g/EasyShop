package org.yearup.data;

import org.yearup.models.Order;
import org.yearup.models.Profile;
import org.yearup.models.ShoppingCart;

import java.util.List;

public interface OrderDao
{
    // turns the cart into an order (shipping to the profile's address) and empties the cart
    Order checkout(int userId, Profile shipTo, ShoppingCart cart);

    List<Order> getByUserId(int userId);
}
