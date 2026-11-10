package com.ccomp.br.domain.events.guests.dto;

public record GuestImageItem(Long guestId, String contentType, byte[] content) {}
