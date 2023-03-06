package com.example.entity;

import com.example.enums.Action;
import com.example.enums.Label;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

@Entity
@Table(name = "admin_history")
public class AdminHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long adminId;

    @Enumerated(value = EnumType.STRING)
    @Column
    private Action action;

    @Enumerated(value = EnumType.STRING)
    @Column
    private Label label;

    @Column
    private String value;
}
