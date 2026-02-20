package com.aktiia.bidapplication.model.dto.response;

import lombok.Builder;

public record AuthResponse(String token,
                           String tokenType,
                           String username,
                           String email) {

    @Builder
    public AuthResponse{}
}
