package com.fashionshop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfig {

    @Id
    private String key;

    @Column(nullable = false)
    private String value;

    private String description;
}
