// License: GPL. For details, see LICENSE file.
package com.kaart.openqa.profiles.keepright;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.openstreetmap.josm.testutils.JOSMTestRules;

class KeepRightInformationTest {
    @TempDir
    File cache;

    @RegisterExtension
    static JOSMTestRules rule = new JOSMTestRules();

    @Test
    void testBuildDefaultPref() throws IOException {
        ArrayList<String> defaultPref = new KeepRightInformation(cache.getCanonicalPath()).buildDefaultPref();
        assertEquals(KeepRightInformation.errors.size(), defaultPref.size());
    }
}
