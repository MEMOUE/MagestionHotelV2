package com.mghbackend.service;

import com.mghbackend.exception.BusinessException;

import com.mghbackend.dto.*;
import com.mghbackend.dto.request.CreateUserRequest;
import com.mghbackend.dto.request.UpdatePasswordRequest;
import com.mghbackend.entity.*;
import com.mghbackend.enums.TypePermission;
import com.mghbackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

	private final UserRepository userRepository;
	private final HotelRepository hotelRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;

	public UserDto createUser(Long hotelId, CreateUserRequest request) {
		Hotel hotel = hotelRepository.findById(hotelId)
				.orElseThrow(() -> new BusinessException("Hôtel non trouvé"));

		if (userRepository.existsByEmail(request.getEmail())) {
			throw new BusinessException("Un utilisateur avec cet email existe déjà");
		}

		if (userRepository.existsByUsername(request.getUsername())) {
			throw new BusinessException("Un utilisateur avec ce nom d'utilisateur existe déjà");
		}

		User user = new User();
		user.setUsername(request.getUsername());
		user.setEmail(request.getEmail());
		user.setFirstName(request.getFirstName());
		user.setLastName(request.getLastName());
		user.setPhone(request.getPhone());
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setHotel(hotel);
		user.setActive(true);

		// Assigner les rôles
		Set<Role> roles = roleRepository.findByNameIn(request.getRoleNames());
		if (roles.size() != request.getRoleNames().size()) {
			throw new BusinessException("Un ou plusieurs rôles sont invalides");
		}
		user.setRoles(roles);

		User savedUser = userRepository.save(user);
		return convertToDto(savedUser);
	}

	@Transactional(readOnly = true)
	public UserDto getUserById(Long id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new BusinessException("Utilisateur non trouvé"));
		return convertToDto(user);
	}

	@Transactional(readOnly = true)
	public UserDto getUserByEmail(String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new BusinessException("Utilisateur non trouvé"));
		return convertToDto(user);
	}

	@Transactional(readOnly = true)
	public UserDto getUserByUsername(String username) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException("Utilisateur non trouvé"));
		return convertToDto(user);
	}

	@Transactional(readOnly = true)
	public List<UserDto> getUsersByHotel(Long hotelId) {
		Hotel hotel = hotelRepository.findById(hotelId)
				.orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
		return userRepository.findByHotel(hotel).stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<UserDto> getActiveUsersByHotel(Long hotelId) {
		Hotel hotel = hotelRepository.findById(hotelId)
				.orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
		return userRepository.findByHotelAndActiveTrue(hotel).stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<UserDto> searchUsers(Long hotelId, String keyword) {
		Hotel hotel = hotelRepository.findById(hotelId)
				.orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
		return userRepository.searchByHotelAndKeyword(hotel, keyword).stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());
	}

	public UserDto updateUser(Long id, UserDto userDto) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new BusinessException("Utilisateur non trouvé"));

		// Vérifier l'unicité de l'email si changé
		if (!user.getEmail().equals(userDto.getEmail()) &&
				userRepository.existsByEmail(userDto.getEmail())) {
			throw new BusinessException("Un utilisateur avec cet email existe déjà");
		}

		// Vérifier l'unicité du username si changé
		if (!user.getUsername().equals(userDto.getUsername()) &&
				userRepository.existsByUsername(userDto.getUsername())) {
			throw new BusinessException("Un utilisateur avec ce nom d'utilisateur existe déjà");
		}

		user.setUsername(userDto.getUsername());
		user.setEmail(userDto.getEmail());
		user.setFirstName(userDto.getFirstName());
		user.setLastName(userDto.getLastName());
		user.setPhone(userDto.getPhone());

		User updatedUser = userRepository.save(user);
		return convertToDto(updatedUser);
	}

	public void updateUserRoles(Long userId, Set<String> roleNames) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException("Utilisateur non trouvé"));

		Set<Role> roles = roleRepository.findByNameIn(roleNames);
		if (roles.size() != roleNames.size()) {
			throw new BusinessException("Un ou plusieurs rôles sont invalides");
		}

		user.setRoles(roles);
		userRepository.save(user);
	}

	public void changePassword(Long userId, UpdatePasswordRequest request) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException("Utilisateur non trouvé"));

		if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
			throw new BusinessException("Ancien mot de passe incorrect");
		}

		user.setPassword(passwordEncoder.encode(request.getNewPassword()));
		userRepository.save(user);
	}

	public void toggleActiveStatus(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException("Utilisateur non trouvé"));
		user.setActive(!user.getActive());
		userRepository.save(user);
	}

	@Transactional(readOnly = true)
	public Set<TypePermission> getUserPermissions(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException("Utilisateur non trouvé"));

		return user.getRoles().stream()
				.flatMap(role -> role.getPermissions().stream())
				.collect(Collectors.toSet());
	}

	private UserDto convertToDto(User user) {
		UserDto dto = new UserDto();
		dto.setId(user.getId());
		dto.setUsername(user.getUsername());
		dto.setEmail(user.getEmail());
		dto.setFirstName(user.getFirstName());
		dto.setLastName(user.getLastName());
		dto.setPhone(user.getPhone());
		dto.setActive(user.getActive());
		dto.setHotelId(user.getHotel().getId());
		dto.setHotelName(user.getHotel().getName());
		dto.setRoleNames(user.getRoles().stream()
				.map(Role::getName)
				.collect(Collectors.toSet()));
		dto.setCreatedAt(user.getCreatedAt());
		dto.setUpdatedAt(user.getUpdatedAt());
		return dto;
	}
}
