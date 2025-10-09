package com.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String name;
    private String email;
	private String password;
    // we don’t include password in DTO — we never expose it in API responses.
}
