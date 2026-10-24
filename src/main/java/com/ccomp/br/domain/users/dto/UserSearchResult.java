package com.ccomp.br.domain.users.dto;

import com.ccomp.br.shared.dto.UserItemDTO;

public record UserSearchResult(UserItemDTO user, Double similarity) {
}
