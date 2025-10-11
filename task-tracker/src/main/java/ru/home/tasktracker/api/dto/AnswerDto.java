package ru.home.tasktracker.api.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerDto {

    private Boolean answer;

    public static AnswerDto setAnswer(Boolean answer) {
        return builder()
                .answer(answer)
                .build();
    }
}
