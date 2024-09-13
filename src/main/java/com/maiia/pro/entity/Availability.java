package com.maiia.pro.entity;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Getter
@Setter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Availability {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;
    private Integer practitionerId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    public Availability(Integer practitionerId, LocalDateTime startDate, LocalDateTime endDate) {
        this.practitionerId = practitionerId;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
