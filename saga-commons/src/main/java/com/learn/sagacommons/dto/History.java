package com.learn.sagacommons.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class History {

    private String source;
    private String status;
    private String message;
    private LocalDateTime createdAt;
}
