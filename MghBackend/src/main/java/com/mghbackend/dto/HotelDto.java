package com.mghbackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotelDto {
	private Long id;

	@NotBlank(message = "Le nom de l'hôtel est obligatoire")
	@Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
	private String name;

	@NotBlank(message = "L'email est obligatoire")
	@Email(message = "Format d'email invalide")
	private String email;

	private String phone;
	private String address;
	private String logoUrl;
	private String taxNumber;

	@Email(message = "Format d'email invalide")
	private String ownerEmail;

	private Boolean active;
	private LocalDateTime subscriptionEnd;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private List<UserDto> users;
}