// License: GPL. For details, see LICENSE file.
package com.kaart.openqa.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.kaart.openqa.OpenQACache;

/**
 * Set up and tear down {@link com.kaart.openqa.OpenQACache}
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@ExtendWith(OpenQACacheAnnotation.Extension.class)
public @interface OpenQACacheAnnotation {
    /**
     * Clear the cache between runs
     */
    class Extension implements AfterEachCallback, BeforeEachCallback {

        @Override
        public void afterEach(ExtensionContext context) {
            OpenQACache.clear();
        }

        @Override
        public void beforeEach(ExtensionContext context) {
            this.afterEach(context);
        }
    }
}
