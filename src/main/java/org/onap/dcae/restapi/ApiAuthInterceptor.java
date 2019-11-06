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
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;
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

        X509Certificate[] certificates = (X509Certificate[]) request.getAttribute(CERTIFICATE_X_509);
        SubjectMatcher subjectMatcher = new SubjectMatcher(settings, certificates);

        if(isHttpPortCalledWithAuthTurnedOn(request)){
            if(isHealthcheckCalledFromInsideCluster(request)){
                return true;
            }
            response.getWriter().write("Operation not permitted");
            response.setStatus(400);
            return false;
        }

        if(isCertSubject(subjectMatcher)){
            LOG.debug("Cert and subjectDN is valid. Subject: " + extractSubject(certificates));
            return true;
        }

        if (isBasicAuth()) {
            return validateBasicHeader(request, response);
        }
        return true;
    }

    private String extractSubject(X509Certificate[] certs) {
        return Arrays.stream(certs)
            .map(e -> e.getSubjectDN().getName())
            .collect(Collectors.joining(","));
    }

    private boolean isHttpPortCalledWithAuthTurnedOn(HttpServletRequest request) {
        return !settings.authMethod().equalsIgnoreCase(AuthMethodType.NO_AUTH.value())
                && request.getLocalPort() == settings.httpPort();
    }

    private boolean isHealthcheckCalledFromInsideCluster(HttpServletRequest request) {
        return request.getRequestURI().replaceAll("^/|/$", "").equalsIgnoreCase("healthcheck")
                && request.getServerPort() == settings.httpPort();
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
        LOG.debug("Request is authorized by basic auth. User: " + extractUser(decodeCredentials(authorizationHeader)));
        return true;
    }

    private boolean isCertSubject(SubjectMatcher subjectMatcher) {
        if(subjectMatcher.isCert() && subjectMatcher.match()){
            return true;
        }
        LOG.info(String.format(MESSAGE, settings.certSubjectMatcher()));
        return false;
    }

    private boolean isBasicAuth() {
        return settings.authMethod().equalsIgnoreCase(AuthMethodType.CERT_BASIC_AUTH.value());
    }

    private boolean isAuthorized(String authorizationHeader) {
        try  {
            String decodeCredentials = decodeCredentials(authorizationHeader);
            String providedUser = extractUser(decodeCredentials);
            String providedPassword = extractPassword(decodeCredentials);
            Option<String> maybeSavedPassword = settings.validAuthorizationCredentials().get(providedUser);
            boolean userRegistered = maybeSavedPassword.isDefined();
            return userRegistered && cryptPassword.matches(providedPassword,maybeSavedPassword.get());
        } catch (Exception e) {
            LOG.warn(String.format("Could not check if user is authorized (header: '%s')), probably malformed header.",
                authorizationHeader), e);
            return false;
        }
    }

    private String extractPassword(String decodeCredentials) {
        return decodeCredentials.split(":")[1].trim();
    }

    private String extractUser(String decodeCredentials) {
        return decodeCredentials.split(":")[0].trim();
    }

    private String decodeCredentials(String authorizationHeader) {
        String encodedData = authorizationHeader.split(" ")[1];
        return new String(Base64.getDecoder().decode(encodedData));
    }
}