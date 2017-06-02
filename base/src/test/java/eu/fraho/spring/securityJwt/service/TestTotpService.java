/*
 * MIT Licence
 * Copyright (c) 2017 Simon Frankenberger
 *
 * Please see LICENCE.md for complete licence text.
 */
package eu.fraho.spring.securityJwt.service;

import eu.fraho.spring.securityJwt.AbstractTest;
import eu.fraho.spring.securityJwt.spring.TestApiApplication;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

@Getter
@Slf4j
@SpringBootTest(classes = TestApiApplication.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class TestTotpService extends AbstractTest {
    @BeforeClass
    public static void beforeClass() throws IOException {
        AbstractTest.beforeHmacClass();
    }

    @Test
    public void testCreateSecret() {
        String secret = totpService.generateSecret();
        Assert.assertNotNull("No secret generated", secret);
        Assert.assertNotEquals("No secret generated", 0, secret.length());
    }

    @Test
    public void testVerifyCode() {
        String secret = totpService.generateSecret();
        Assert.assertFalse("Empty token used", totpService.verifyCode(secret, 0));
    }

    @Test
    public void testVarianceLowerBounds() throws Exception {
        withTempTotpServiceField("totpVariance", 0, () -> {
            totpService.afterPropertiesSet();
            Assert.assertEquals("Value should be default", Integer.valueOf(3), totpService.getTotpVariance());
        });
    }

    @Test
    public void testVarianceUpperBounds() throws Exception {
        withTempTotpServiceField("totpVariance", 100, () -> {
            totpService.afterPropertiesSet();
            Assert.assertEquals("Value should be default", Integer.valueOf(3), totpService.getTotpVariance());
        });
    }

    @Test
    public void testLengthLowerBounds() throws Exception {
        withTempTotpServiceField("totpLength", 3, () -> {
            totpService.afterPropertiesSet();
            Assert.assertEquals("Value should be default", Integer.valueOf(16), totpService.getTotpLength());
        });
    }

    @Test
    public void testLengthUpperBounds() throws Exception {
        withTempTotpServiceField("totpLength", 100, () -> {
            totpService.afterPropertiesSet();
            Assert.assertEquals("Value should be default", Integer.valueOf(16), totpService.getTotpLength());
        });
    }
}