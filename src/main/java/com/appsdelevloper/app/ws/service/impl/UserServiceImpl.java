package com.appsdelevloper.app.ws.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.appsdelevloper.app.ws.exception.UserServiceException;
import com.appsdelevloper.app.ws.io.entity.UserEntity;
import com.appsdelevloper.app.ws.io.repositories.UserRepository;
import com.appsdelevloper.app.ws.service.UserService;
import com.appsdelevloper.app.ws.shared.Utils;
import com.appsdelevloper.app.ws.shared.dto.AddressDTO;
import com.appsdelevloper.app.ws.shared.dto.UserDto;
import com.appsdelevloper.app.ws.ui.model.response.ErrorMessages;

@Service
public class UserServiceImpl implements UserService {

	@Autowired
	UserRepository userRepository;

	@Autowired
	Utils utils; 

	@Autowired
	BCryptPasswordEncoder bCryptPasswordEncoder;

	@Override
	public UserDto createUser(UserDto user) {

		if (userRepository.findByEmail(user.getEmail()) != null)
			throw new RuntimeException("Record already Exists");
		
		for (int i = 0; i < user.getAddresses().size(); i++) {
			AddressDTO address = user.getAddresses().get(i);
			address.setUserDetails(user);
			address.setAddressId(utils.generateAddressId(30));
			user.getAddresses().set(i,  address);
		}

		ModelMapper modelMapper = new ModelMapper ();
		UserEntity userEntity = modelMapper.map(user, UserEntity.class);
		
		String publicUserId = utils.generateUserId(30);
		userEntity.setUserId(publicUserId);
		userEntity.setEncryptedPassword(bCryptPasswordEncoder.encode(user.getPassword()));

		String verficationToken = utils.generateEmailVerificationToken(publicUserId);
		userEntity.setEmailVerficationToken(verficationToken);
		userEntity.setEmailVerificationStatus(false);
		
		
		UserEntity storedUserDetails = userRepository.save(userEntity);
		UserDto returnValue = modelMapper.map(storedUserDetails, UserDto.class);

		return returnValue;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		UserEntity userEntity = userRepository.findByEmail(email);

		if (userEntity == null)
			throw new UsernameNotFoundException(email);

		return new User(userEntity.getEmail(), userEntity.getEncryptedPassword(), userEntity.getEmailVerificationStatus(), 
				true, true, true, new ArrayList<>());
	}

	@Override
	public UserDto getUser(String email) {
		UserEntity userEntity = userRepository.findByEmail(email);
		if (userEntity == null)
			throw new UsernameNotFoundException(email);

		UserDto returnValue = new UserDto();
		BeanUtils.copyProperties(userEntity, returnValue);
		return returnValue;
	}

	@Override
	public UserDto getUserByUserId(String userId) {
		UserDto returnValue = new UserDto();
		UserEntity userEntity = userRepository.findByUserId(userId);
		System.out.println("NEW");
		if (userEntity == null)
			throw new UsernameNotFoundException("User with ID: " + userId + " not found");

		BeanUtils.copyProperties(userEntity, returnValue);

		return returnValue;

	}

	@Override
	public UserDto updateUser(String userId, UserDto user) {
		UserDto returnValue = new UserDto();
		UserEntity userEntity = userRepository.findByUserId(userId);

		if (userEntity == null)
			throw new UserServiceException(ErrorMessages.NO_RECORD_FOUND.getErrorMessage());

		userEntity.setFirstName(user.getFirstName());
		userEntity.setLastName(user.getLastName());

		UserEntity updatedUserDetails = userRepository.save(userEntity);

		BeanUtils.copyProperties(updatedUserDetails, returnValue);

		return returnValue;
	}

	@Override
	public void deleteUser(String userId) {

		UserEntity userEntity = userRepository.findByUserId(userId);

		if (userEntity == null)
			throw new UserServiceException(ErrorMessages.NO_RECORD_FOUND.getErrorMessage());
		
		userRepository.delete(userEntity);

	}

	@Override
	public List<UserDto> getUsers(int page, int limit) {
		List<UserDto> returnValue = new ArrayList<>();
		
		Pageable pageableRequest = PageRequest.of(page, limit);
		
		Page<UserEntity> usersPage= userRepository.findAll(pageableRequest);
		List<UserEntity> users = usersPage.getContent();
		
		for(UserEntity userEntity: users) {
			UserDto userModel  = new UserDto();
			BeanUtils.copyProperties(userEntity, userModel);
			returnValue.add(userModel);
		}
		
		return returnValue;
	}
	

	public List<UserDto> getUsersAddress(int page, int limit) {
		List<UserDto> returnValue = new ArrayList<>();
		
		Pageable pageableRequest = PageRequest.of(page, limit);
		
		Page<UserEntity> usersPage= userRepository.findAll(pageableRequest);
		List<UserEntity> users = usersPage.getContent();
		
		for(UserEntity userEntity: users) {
			UserDto userModel  = new UserDto();
			BeanUtils.copyProperties(userEntity, userModel);
			returnValue.add(userModel);
		}
		
		return returnValue;
	}

	@Override
	public boolean verifyEmailToken(String token) {
		boolean returnValue = false;
		
		UserEntity userEntity = userRepository.findByEmailVerficationToken(token);
		
		if(userEntity != null) {
			boolean hasTokenExpired = Utils.hasTokenExpired(token);
			if(hasTokenExpired) {
				userEntity.setEmailVerficationToken(null);
				userEntity.setEmailVerificationStatus(Boolean.TRUE);
				userRepository.save(userEntity);
				returnValue = true;
			}
		}
		return returnValue;
	}

}
