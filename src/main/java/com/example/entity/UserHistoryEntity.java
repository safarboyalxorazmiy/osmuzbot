package com.example.entity;

import com.example.enums.Action;
import com.example.enums.Label;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

@Entity
@Table(name = "user_history")
public class UserHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @Enumerated(value = EnumType.STRING)
    private Label label;

    @Column
    private Long userId;

    @Column
    private String value;
}
