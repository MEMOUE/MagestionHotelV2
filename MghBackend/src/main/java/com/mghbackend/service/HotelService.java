package com.mghbackend.service;

import com.mghbackend.exception.BusinessException;

import com.mghbackend.dto.*;
import com.mghbackend.dto.request.CreateHotelRequest;
import com.mghbackend.dto.request.UpdatePasswordRequest;
import com.mghbackend.entity.Hotel;
import com.mghbackend.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class HotelService {

	private final HotelRepository hotelRepository;
	private final PasswordEncoder passwordEncoder;

	public HotelDto createHotel(CreateHotelRequest request) {
		// Vérifier si l'email existe déjà
		if (hotelRepository.existsByEmail(request.getEmail())) {
			throw new BusinessException("Un hôtel avec cet email existe déjà");
		}

		// Vérifier si le nom existe déjà
		if (hotelRepository.existsByName(request.getName())) {
			throw new BusinessException("Un hôtel avec ce nom existe déjà");
		}

		Hotel hotel = new Hotel();
		hotel.setName(request.getName());
		hotel.setEmail(request.getEmail());
		hotel.setPhone(request.getPhone());
		hotel.setAddress(request.getAddress());
		hotel.setTaxNumber(request.getTaxNumber());
		hotel.setPassword(passwordEncoder.encode(request.getPassword()));
		hotel.setActive(true);
		hotel.setSubscriptionEnd(LocalDateTime.now().plusYears(1)); // 1 an par défaut

		Hotel savedHotel = hotelRepository.save(hotel);
		return convertToDto(savedHotel);
	}

	@Transactional(readOnly = true)
	public HotelDto getHotelById(Long id) {
		Hotel hotel = hotelRepository.findById(id)
				.orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
		return convertToDto(hotel);
	}

	@Transactional(readOnly = true)
	public HotelDto getHotelByEmail(String email) {
		Hotel hotel = hotelRepository.findByEmail(email)
				.orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
		return convertToDto(hotel);
	}

	@Transactional(readOnly = true)
	public List<HotelDto> getAllHotels() {
		return hotelRepository.findAll().stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<HotelDto> getActiveHotels() {
		return hotelRepository.findByActiveTrue().stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<HotelDto> searchHotels(String keyword) {
		return hotelRepository.searchByKeyword(keyword).stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());
	}

	public HotelDto updateHotel(Long id, HotelDto hotelDto) {
		Hotel hotel = hotelRepository.findById(id)
				.orElseThrow(() -> new BusinessException("Hôtel non trouvé"));

		// Vérifier l'unicité de l'email si changé
		if (!hotel.getEmail().equals(hotelDto.getEmail()) &&
				hotelRepository.existsByEmail(hotelDto.getEmail())) {
			throw new BusinessException("Un hôtel avec cet email existe déjà");
		}

		// Vérifier l'unicité du nom si changé
		if (!hotel.getName().equals(hotelDto.getName()) &&
				hotelRepository.existsByName(hotelDto.getName())) {
			throw new BusinessException("Un hôtel avec ce nom existe déjà");
		}

		hotel.setName(hotelDto.getName());
		hotel.setEmail(hotelDto.getEmail());
		hotel.setPhone(hotelDto.getPhone());
		hotel.setAddress(hotelDto.getAddress());
		hotel.setLogoUrl(hotelDto.getLogoUrl());
		hotel.setTaxNumber(hotelDto.getTaxNumber());

		Hotel updatedHotel = hotelRepository.save(hotel);
		return convertToDto(updatedHotel);
	}

	public void changePassword(Long hotelId, UpdatePasswordRequest request) {
		Hotel hotel = hotelRepository.findById(hotelId)
				.orElseThrow(() -> new BusinessException("Hôtel non trouvé"));

		if (!passwordEncoder.matches(request.getOldPassword(), hotel.getPassword())) {
			throw new BusinessException("Ancien mot de passe incorrect");
		}

		hotel.setPassword(passwordEncoder.encode(request.getNewPassword()));
		hotelRepository.save(hotel);
	}

	public void toggleActiveStatus(Long hotelId) {
		Hotel hotel = hotelRepository.findById(hotelId)
				.orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
		hotel.setActive(!hotel.getActive());
		hotelRepository.save(hotel);
	}

	public void extendSubscription(Long hotelId, int months) {
		Hotel hotel = hotelRepository.findById(hotelId)
				.orElseThrow(() -> new BusinessException("Hôtel non trouvé"));

		LocalDateTime currentEnd = hotel.getSubscriptionEnd();
		LocalDateTime newEnd = (currentEnd != null && currentEnd.isAfter(LocalDateTime.now()))
				? currentEnd.plusMonths(months)
				: LocalDateTime.now().plusMonths(months);

		hotel.setSubscriptionEnd(newEnd);
		hotelRepository.save(hotel);
	}

	private HotelDto convertToDto(Hotel hotel) {
		HotelDto dto = new HotelDto();
		dto.setId(hotel.getId());
		dto.setName(hotel.getName());
		dto.setEmail(hotel.getEmail());
		dto.setPhone(hotel.getPhone());
		dto.setAddress(hotel.getAddress());
		dto.setLogoUrl(hotel.getLogoUrl());
		dto.setTaxNumber(hotel.getTaxNumber());
		dto.setActive(hotel.getActive());
		dto.setSubscriptionEnd(hotel.getSubscriptionEnd());
		dto.setCreatedAt(hotel.getCreatedAt());
		dto.setUpdatedAt(hotel.getUpdatedAt());
		return dto;
	}
}
