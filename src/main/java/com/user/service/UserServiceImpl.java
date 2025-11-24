package com.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import com.user.dto.UserDto;
import com.user.model.Address;
import com.user.model.User;
import com.user.repository.AddressRepository;
import com.user.repository.UserRepository;
//import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.user.utility.UserMapper;

import java.util.Optional;
@Service
public class UserServiceImpl implements UserService {


          @Autowired
          UserRepository userRepository;
      /*  private final AddressRepository addressRepository;
        private final PasswordEncoder passwordEncoder;*/

        public UserDto registerUser(UserDto userdto) {
            User user=  UserMapper.fromDto(userdto);
            // user.setPassword(passwordEncoder.encode(userdto.getPassword()));
            user.setPasswordHash("default hash");
            return UserMapper.toDto(userRepository.save(user));
        }

       /* public Optional<User> getUserByEmail(String email) {
            return userRepository.findByEmail(email);
        }

        public User updateProfile(User user) {
            return userRepository.save(user);
        }

        public Address addAddress(User user, Address address) {
            address.setUser(user);
            return addressRepository.save(address);
        }

        public void deleteAddress(Long addressId) {
            addressRepository.deleteById(addressId);
        }*/
    }

