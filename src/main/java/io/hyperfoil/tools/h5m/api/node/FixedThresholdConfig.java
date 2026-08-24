package io.hyperfoil.tools.h5m.api.node;

import jakarta.validation.constraints.AssertTrue;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Configuration for the Fixed Threshold detection algorithm")
public record FixedThresholdConfig(Double min, Double max, Boolean minInclusive, Boolean maxInclusive, String fingerprintFilter) implements NodeConfiguration {

    @AssertTrue(message = "min must be less than or equal to max")
    boolean isMinLessThanOrEqualToMax() {
        return min == null || max == null || min <= max;
    }
}
