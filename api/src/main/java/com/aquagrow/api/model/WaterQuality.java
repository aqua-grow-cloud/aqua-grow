package com.aquagrow.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaterQuality {
    @Column(name = "do_level")
    private Double doLevel;

    @Column(name = "ph_level")
    private Double ph;

    private Double temperature;
    private Double ammonia;
    private Double salinity;
    private Double turbidity;
}
