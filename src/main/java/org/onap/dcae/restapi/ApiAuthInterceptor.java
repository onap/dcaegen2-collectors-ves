/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2018 - 2019 Nokia. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.dcae.restapi;

import io.vavr.control.Option;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.common.configuration.AuthMethodType;
import org.onap.dcae.common.configuration.SubjectMatcher;
import org.onap.dcaegen2.services.sdk.security.CryptPassword;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Component
public class ApiAuthInterceptor extends HandlerInterceptorAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(ApiAuthInterceptor.class);
    private static final String CERTIFICATE_X_509 = "javax.servlet.request.X509Certificate";
    private static final String MESSAGE = "SubjectDN didn't match with any regexp from %s";
    private final CryptPassword cryptPassword = new CryptPassword();
    private final ApplicationSettings settings;
    private Logger errorLogger;

    public ApiAuthInterceptor(ApplicationSettings applicationSettings, Logger errorLogger) {
        this.settings = applicationSettings;
        this.errorLogger = errorLogger;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws IOException {

        SubjectMatcher subjectMatcher = new SubjectMatcher(settings,(X509Certificate[]) request.getAttribute(CERTIFICATE_X_509));

        if(!settings.authMethod().equalsIgnoreCase(AuthMethodType.NO_AUTH.value()) && request.getServerPort() == settings.httpPort() ){
            if(request.getRequestURI().replaceAll("^/|/$", "").equalsIgnoreCase("healthcheck")){
                return true;
            }
            response.getWriter().write("Operation not permitted");
            response.setStatus(400);
            return false;
        }

        if(settings.authMethod().equalsIgnoreCase(AuthMethodType.CERT_ONLY.value())){
            return validateCertRequest(response, subjectMatcher);
        }

        if(isCertSubject(subjectMatcher)){
            return true;
        }

        if (isBasicAuth() ) {
            return validateBasicHeader(request, response);
        }
        return true;
    }

    private boolean validateBasicHeader(HttpServletRequest request, HttpServletResponse response)
        throws IOException {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !isAuthorized(authorizationHeader)) {
            response.setStatus(401);
            errorLogger.error("EVENT_RECEIPT_FAILURE: Unauthorized user");
            response.getWriter().write(ApiException.UNAUTHORIZED_USER.toJSON().toString());
            return false;
        }
        LOG.info("Request is authorized by basic auth");
        return true;
    }

    private boolean validateCertRequest(HttpServletResponse response, SubjectMatcher subjectMatcher)
        throws IOException {
        if (!isCertSubject(subjectMatcher)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(String.format(MESSAGE, settings.certSubjectMatcher()));
            return false;
        }
        LOG.info("Cert and subjectDN is valid");
        return true;
    }

    private boolean isCertSubject(SubjectMatcher subjectMatcher) {
        if(subjectMatcher.isCert() && subjectMatcher.match()){
            LOG.info("Cert and subjectDN is valid");
            return true;
        }
        LOG.info(String.format(MESSAGE, settings.certSubjectMatcher()));
        return false;
    }

    private boolean isBasicAuth() {
        return settings.authMethod().equalsIgnoreCase(AuthMethodType.BASIC_AUTH.value())
            || settings.authMethod().equalsIgnoreCase(AuthMethodType.CERT_BASIC_AUTH.value());
    }

    private boolean isAuthorized(String authorizationHeader) {
        try  {
            String encodedData = authorizationHeader.split(" ")[1];
            String decodedData = new String(Base64.getDecoder().decode(encodedData));
            String providedUser = decodedData.split(":")[0].trim();
            String providedPassword = decodedData.split(":")[1].trim();
            Option<String> maybeSavedPassword = settings.validAuthorizationCredentials().get(providedUser);
            boolean userRegistered = maybeSavedPassword.isDefined();
            return userRegistered && cryptPassword.matches(providedPassword,maybeSavedPassword.get());
        } catch (Exception e) {
            LOG.warn(String.format("Could not check if user is authorized (header: '%s')), probably malformed header.",
                authorizationHeader), e);
            return false;
        }
    }
}