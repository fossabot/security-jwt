package eu.fraho.spring.securityJwt.tokenService;

import com.nimbusds.jose.JOSEException;
import eu.fraho.spring.securityJwt.AbstractTest;
import eu.fraho.spring.securityJwt.dto.AccessToken;
import eu.fraho.spring.securityJwt.dto.JwtUser;
import eu.fraho.spring.securityJwt.dto.RefreshToken;
import eu.fraho.spring.securityJwt.service.JwtTokenService;
import eu.fraho.spring.securityJwt.spring.TestApiApplication;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@Getter
@Setter(AccessLevel.NONE)
@Slf4j
@SpringBootTest(properties = "spring.config.location=classpath:memcache-test-other.yaml",
        classes = TestApiApplication.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class TestJwtServiceMemcacheOther extends AbstractTest {
    @Autowired
    protected JwtTokenService jwtTokenService = null;

    @BeforeClass
    public static void beforeClass() throws Exception {
        AbstractTest.beforeHmacClass();
    }

    @Test
    public void testParseUser() throws JOSEException {
        JwtUser userIn = getJwtUser();
        AccessToken token = jwtTokenService.generateToken(userIn);
        Assert.assertNotNull("No token generated", token.getToken());

        Optional<JwtUser> tmpUserOut = jwtTokenService.parseUser(token.getToken());
        Assert.assertTrue("User could not be parsed from token", tmpUserOut.isPresent());

        JwtUser userOut = tmpUserOut.get();
        Assert.assertNotNull("Parsed user without id", userOut.getId());
        Assert.assertEquals("Parsed user with wrong id", userIn.getId(), userOut.getId());

        Assert.assertNotNull("Parsed user without username", userOut.getUsername());
        Assert.assertEquals("Parsed user with wrong username", userIn.getUsername(), userOut.getUsername());

        Assert.assertNotNull("Parsed user without role", userOut.getAuthority());
        Assert.assertEquals("Parsed user with wrong role", userIn.getAuthority(), userOut.getAuthority());
    }

    @Test
    public void testGetToken() throws JOSEException {
        HttpServletRequest req;

        req = MockMvcRequestBuilders.get("/").header("Authorization", "Bearer foobar").buildRequest(null);
        Optional<String> token = jwtTokenService.getToken(req);
        Assert.assertTrue("Could not extract token from header", token.isPresent());
        Assert.assertEquals("Could not extract token from header", "foobar", token.get());

        req = MockMvcRequestBuilders.get("/").header("Authorization", "foobar").buildRequest(null);
        token = jwtTokenService.getToken(req);
        Assert.assertTrue("Could not extract token from header", token.isPresent());
        Assert.assertEquals("Could not extract token from header", "foobar", token.get());

        req = MockMvcRequestBuilders.get("/").buildRequest(null);
        token = jwtTokenService.getToken(req);
        Assert.assertFalse("Could extract token from header", token.isPresent());
    }

    @Test(timeout = 10_000L)
    public void testExpireToken() throws JOSEException, InterruptedException {
        AccessToken token = jwtTokenService.generateToken(getJwtUser());
        Assert.assertNotNull("No token generated", token);

        Assert.assertTrue("Token expired", jwtTokenService.validateToken(token));
        Thread.sleep(jwtTokenService.getExpiration() * 1000 + 100);
        Assert.assertFalse("Token didn't expire", jwtTokenService.validateToken(token));
    }

    @Test(timeout = 10_000L)
    public void testExpireRefreshToken() throws JOSEException, InterruptedException {
        String jsmith = "jsmith";
        RefreshToken token = jwtTokenService.generateRefreshToken(jsmith);
        Assert.assertNotNull("No token generated", token.getToken());
        Thread.sleep(jwtTokenService.getRefreshExpiration() * 1000 + 100);
        Assert.assertFalse("Token didn't expire", jwtTokenService.useRefreshToken(jsmith, token));
    }
}