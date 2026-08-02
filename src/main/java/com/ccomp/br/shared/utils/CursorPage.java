package com.ccomp.br.shared.utils;

import java.time.LocalDateTime;
import java.util.List;

public record CursorPage<T>(
        List<T> content,
        String nextCursor,
        String previousCursor) {
}
