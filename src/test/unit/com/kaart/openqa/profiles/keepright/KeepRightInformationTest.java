// License: GPL. For details, see LICENSE file.
package com.kaart.openqa.profiles.keepright;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import com.kaart.openqa.annotations.OpenQACacheAnnotation;

/**
 * Test class for {@link KeepRightInformation}
 */
@OpenQACacheAnnotation
class KeepRightInformationTest {
    @RegisterExtension
    static JOSMTestRules rule = new JOSMTestRules();

    /**
     * Test the default KeepRight preferences
     */
    @Test
    void testBuildDefaultPref() {
        ArrayList<String> defaultPref = new KeepRightInformation().buildDefaultPref();
        assertEquals(KeepRightInformation.errors.size(), defaultPref.size());
    }
}
