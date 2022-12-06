// License: GPL. For details, see LICENSE file.
package com.kaart.openqa.profiles.keepright;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

import com.kaart.openqa.OpenQADataSet;

/**
 * Test class for {@link com.kaart.openqa.OpenQADataSet}
 */
class OpenQADataSetTest {
    /**
     * Non-regression test for #22548 comment 5
     */
    @Test
    void testNonRegression22548Comment5() {
        OpenQADataSet<Long, KeepRightNode> testSet = new OpenQADataSet<>();
        assertDoesNotThrow(() -> testSet.mergeFrom(null));
    }
}
