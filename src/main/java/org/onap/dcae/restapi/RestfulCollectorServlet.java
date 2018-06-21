
/*
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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

import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletException;

import org.apache.tomcat.util.codec.binary.Base64;
import org.onap.dcae.commonFunction.CommonStartup;
import org.onap.dcae.commonFunction.VESLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.nsa.apiServer.CommonServlet;
import com.att.nsa.configs.ConfigDbException;
import com.att.nsa.drumlin.service.framework.routing.DrumlinRequestRouter;
import com.att.nsa.drumlin.service.framework.routing.playish.DrumlinPlayishRoutingFileSource;
import com.att.nsa.drumlin.service.standards.HttpStatusCodes;
import com.att.nsa.drumlin.till.nv.rrNvReadable;
import com.att.nsa.drumlin.till.nv.rrNvReadable.loadException;
import com.att.nsa.drumlin.till.nv.rrNvReadable.missingReqdSetting;
import com.att.nsa.security.NsaAuthenticator;

import com.att.nsa.security.authenticators.SimpleAuthenticator;
import com.att.nsa.security.db.simple.NsaSimpleApiKey;

public class RestfulCollectorServlet extends CommonServlet
{

	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger ( RestfulCollectorServlet.class );

	private static String authCredentialsList;

	public RestfulCollectorServlet ( rrNvReadable settings ) throws loadException, missingReqdSetting
	{
		super ( settings, "collector", false );
		authCredentialsList = settings.getString(CommonStartup.KSETTING_AUTHLIST, null);
	}




	/**
	 * This is called once at server start. Use it to init any shared objects and setup the route mapping.
	 */
	@Override
	protected void servletSetup () throws rrNvReadable.missingReqdSetting, rrNvReadable.invalidSettingValue, ServletException
	{
		super.servletSetup ();

		try {
			// the base class provides a bunch of things like API authentication and ECOMP compliant
			// logging. The Restful Collector likely doesn't need API authentication, so for now,
			// we init the base class services with an in-memory (and empty!) config DB.
			commonServletSetup ( ConfigDbType.MEMORY );

			VESLogger.setUpEcompLogging();

			// setup the servlet routing and error handling
			final DrumlinRequestRouter drr = getRequestRouter ();

			// you can tell the request router what to do when a particular kind of exception is thrown.
			drr.setHandlerForException(IllegalArgumentException.class,
									   (ctx, cause) -> sendJsonReply (ctx, HttpStatusCodes.k400_badRequest, cause.getMessage() ));

			// load the routes from the config file
			final URL routes = findStream ( "routes.conf" );
			if ( routes == null ) throw new rrNvReadable.missingReqdSetting ( "No routing configuration." );
			final DrumlinPlayishRoutingFileSource drs = new DrumlinPlayishRoutingFileSource ( routes );
			drr.addRouteSource ( drs );

			if (CommonStartup.authflag > 0) {
				NsaAuthenticator<NsaSimpleApiKey> NsaAuth;
				NsaAuth = createAuthenticator(authCredentialsList);

				this.getSecurityManager().addAuthenticator(NsaAuth);
			}

			log.info ( "Restful Collector Servlet is up." );
		}
		catch ( SecurityException | IOException | ConfigDbException e ) {
			throw new ServletException ( e );
		}
	}

	public NsaAuthenticator<NsaSimpleApiKey> createAuthenticator(String authCredentials) {
		NsaAuthenticator<NsaSimpleApiKey> authenticator = new SimpleAuthenticator();
		if (authCredentials != null) {
			String authpair[] = authCredentials.split("\\|");
			for (String pair : authpair) {
				String lineid[] = pair.split(",");
				String listauthid = lineid[0];
				String listauthpwd = new String(Base64.decodeBase64(lineid[1]));
				((SimpleAuthenticator) authenticator).add(listauthid, listauthpwd);
			}

		} else {
			((SimpleAuthenticator) authenticator).add("admin", "collectorpasscode");
		}
		return authenticator;
	}

}

