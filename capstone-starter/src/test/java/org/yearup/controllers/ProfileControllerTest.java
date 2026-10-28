package org.yearup.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.data.ProfileDao;
import org.yearup.data.UserDao;
import org.yearup.models.Profile;
import org.yearup.models.ProfileUpdateRequest;
import org.yearup.models.User;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProfileControllerTest
{
    private ProfileDao profileDao;
    private UserDao userDao;
    private ProfileController controller;
    private final Principal principal = () -> "alice";

    @BeforeEach
    void setUp()
    {
        profileDao = mock(ProfileDao.class);
        userDao = mock(UserDao.class);
        controller = new ProfileController(profileDao, userDao);

        User user = new User();
        user.setId(7);
        when(userDao.getByUserName("alice")).thenReturn(user);
    }

    @Test
    void getProfile_returnsOnlyTheAuthenticatedUsersProfile()
    {
        Profile expected = new Profile(7, "Alice", "Example", "", "", "", "", "", "");
        when(profileDao.getByUserId(7)).thenReturn(expected);

        assertSame(expected, controller.getProfile(principal));

        verify(profileDao).getByUserId(7);
        verifyNoMoreInteractions(profileDao);
    }

    @Test
    void getProfile_returns404WhenProfileIsMissing()
    {
        when(profileDao.getByUserId(7)).thenReturn(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> controller.getProfile(principal));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void getProfile_returns401WithoutPrincipal()
    {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> controller.getProfile(null));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        verifyNoInteractions(profileDao);
    }

    @Test
    void getProfile_returns401WhenAccountDoesNotExist()
    {
        when(userDao.getByUserName("alice")).thenReturn(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> controller.getProfile(principal));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        verifyNoInteractions(profileDao);
    }

    @Test
    void updateProfile_usesTheAuthenticatedUsersId()
    {
        when(profileDao.getByUserId(7)).thenReturn(new Profile());
        ProfileUpdateRequest request = validRequest();

        Profile updated = controller.updateProfile(request, principal);

        assertEquals(7, updated.getUserId());
        assertEquals("Alice", updated.getFirstName());
        assertEquals("alice@example.com", updated.getEmail());
        verify(profileDao).update(argThat(profile ->
                profile.getUserId() == 7
                        && profile.getFirstName().equals("Alice")
                        && profile.getEmail().equals("alice@example.com")));
    }

    @Test
    void updateProfile_returns404WithoutCreatingMissingProfile()
    {
        when(profileDao.getByUserId(7)).thenReturn(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> controller.updateProfile(validRequest(), principal));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(profileDao, never()).update(any());
    }

    @Test
    void updateProfile_doesNotWriteIfDatabaseUserMissing()
    {
        when(userDao.getByUserName("alice")).thenReturn(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> controller.updateProfile(validRequest(), principal));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        verifyNoInteractions(profileDao);
    }

    @Test
    void updateProfile_doesNotClaimSuccessOnDatabaseFailure()
    {
        when(profileDao.getByUserId(7)).thenReturn(new Profile());
        doThrow(new IllegalStateException("Database unavailable"))
                .when(profileDao).update(any());

        assertThrows(IllegalStateException.class,
                () -> controller.updateProfile(validRequest(), principal));
    }

    private ProfileUpdateRequest validRequest()
    {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFirstName("Alice");
        request.setLastName("Example");
        request.setPhone("");
        request.setEmail("alice@example.com");
        request.setAddress("");
        request.setCity("");
        request.setState("");
        request.setZip("");
        return request;
    }
}
