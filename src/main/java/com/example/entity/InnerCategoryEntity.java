package com.example.entity;

import jakarta.persistence.*;
import jdk.jfr.Category;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

@Getter
@Setter

@Entity
@Table(name = "inner_category")
public class InnerCategoryEntity {
    @Id
    @GeneratedValue(generator = "inner_category_uuid")
    @GenericGenerator(name = "inner_category_uuid", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    @Column(name = "nameuz", unique = true)
    private String nameUz;
    @Column(name = "nameru", unique = true)
    private String nameRu;

    @Column(name = "category_id")
    private Long categoryId;

    @ManyToOne
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private CategoryEntity category;
}
