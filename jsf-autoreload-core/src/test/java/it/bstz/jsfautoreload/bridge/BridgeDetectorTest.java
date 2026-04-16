package it.bstz.jsfautoreload.bridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BridgeDetectorTest {

    @Test
    void detect_returnsNonNullBridgePair() {
        // In test classpath both javax and jakarta are available.
        // Jakarta should be detected first (jakarta-first order).
        BridgePair pair = BridgeDetector.detect();
        assertNotNull(pair);
        assertNotNull(pair.jsf());
        assertNotNull(pair.servlet());
    }

    @Test
    void detect_jakartaFirstOrder() {
        // When both namespaces are present, jakarta should be selected
        BridgePair pair = BridgeDetector.detect();
        // The bridge pair should use jakarta implementations when both are available
        assertTrue(pair.jsf().projectStageParamName().contains("jakarta"),
                "Should prefer jakarta namespace when both are available");
    }
}
