package com.example.entity;

import com.example.enums.Language;
import com.example.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    private Long chatId;

    @Column
    private String firstName;

    @Column
    private String lastName;

    @Column
    private LocalDateTime registerAt;

    @Enumerated(value = EnumType.STRING)
    @Column
    private Role role;

    @Enumerated(value = EnumType.STRING)
    @Column
    private Language language;
}
