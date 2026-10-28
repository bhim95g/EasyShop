package org.yearup.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.data.ProductDao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ProductsControllerTest {
    private ProductDao productDao;
    private ProductsController controller;

    @BeforeEach
    void setUp() {
        productDao = mock(ProductDao.class);
        controller = new ProductsController(productDao);
    }

    @Test
    void getById_returns404WhenProductDoesNotExist() {
        when(productDao.getById(999)).thenReturn(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> controller.getById(999));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void updateProduct_returns404WhenProductDoesNotExist() {
        when(productDao.getById(999)).thenReturn(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> controller.updateProduct(999, null));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(productDao, never()).update(anyInt(), any());
    }

    @Test
    void deleteProduct_returns404WhenProductDoesNotExist() {
        when(productDao.getById(999)).thenReturn(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> controller.deleteProduct(999));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(productDao, never()).delete(anyInt());
    }
}
