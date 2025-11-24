package com.user.service;

import com.user.dto.UserDto;
import com.user.model.Address;
import com.user.model.User;

import java.util.Optional;

public interface UserService {
    public UserDto registerUser(UserDto user) ;
  /*  public Optional<User> getUserByEmail(String email);
    public User updateProfile(User user) ;
    public Address addAddress(User user, Address address) ;
    public void deleteAddress(Long addressId) ;*/

}
