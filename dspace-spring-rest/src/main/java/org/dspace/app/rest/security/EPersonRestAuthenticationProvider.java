package org.dspace.app.rest.security;

import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class EPersonRestAuthenticationProvider implements AuthenticationProvider{

    protected AuthenticationService authenticationService = AuthenticateServiceFactory.getInstance().getAuthenticationService();
    private static final Logger log = LoggerFactory.getLogger(EPersonRestAuthenticationProvider.class);

    @Autowired
    private HttpServletRequest request;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Context context = null;

        try {
            context = new Context();
            String name = authentication.getName();
            String password = authentication.getCredentials().toString();
            HttpServletRequest httpServletRequest = request;
            List<GrantedAuthority> grantedAuthorities = new ArrayList<>();


            int implicitStatus = authenticationService.authenticateImplicit(context, null, null, null, httpServletRequest);

            if (implicitStatus == AuthenticationMethod.SUCCESS) {
                log.info(LogManager.getHeader(context, "login", "type=implicit"));
                return new DSpaceAuthentication(name, password, grantedAuthorities);
            } else {
                int authenticateResult = authenticationService.authenticate(context, name, password, null, httpServletRequest);
                if (AuthenticationMethod.SUCCESS == authenticateResult) {

                    log.info(LogManager
                            .getHeader(context, "login", "type=explicit"));

                    return new DSpaceAuthentication(name, password, grantedAuthorities);
                } else {
                    log.info(LogManager.getHeader(context, "failed_login", "email="
                            + name + ", result="
                            + authenticateResult));
                    throw new BadCredentialsException("Login failed");
                }
            }
        } catch (BadCredentialsException e)
        {
            throw e;
        } catch (Exception e) {
            log.error("Error while authenticating in the rest api", e);
        } finally {
            if (context != null && context.isValid()) {
                try {
                    context.complete();
                } catch (SQLException e) {
                    log.error(e.getMessage() + " occurred while trying to close", e);
                }
            }
        }

        return null;
    }



    public boolean supports(Class<?> authentication) {
        return authentication.equals(DSpaceAuthentication.class);
    }
}
