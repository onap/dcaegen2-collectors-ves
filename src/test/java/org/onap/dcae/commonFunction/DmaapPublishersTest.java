/*-
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2018 Nokia. All rights reserved.
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
package org.onap.dcae.commonFunction;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.att.nsa.cambria.client.CambriaBatchingPublisher;
import com.att.nsa.cambria.client.CambriaPublisher;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class DmaapPublishersTest {

    @Mock
    private CambriaPublisherFactory publisherFactory;
    @Mock
    private CambriaBatchingPublisher cambriaPublisher;
    private DmaapPublishers cut;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws MalformedURLException, GeneralSecurityException {
        given(publisherFactory.createCambriaPublisher(anyString())).willReturn(cambriaPublisher);
        cut = DmaapPublishers.create(publisherFactory);
    }

    @Test
    public void getByStreamIdShouldUseCachedItem() throws IOException, GeneralSecurityException {
        // given
        String streamId = "sampleStream";

        // when
        CambriaBatchingPublisher firstPublisher = cut.getByStreamId(streamId);
        CambriaBatchingPublisher secondPublisher = cut.getByStreamId(streamId);

        // then
        verify(publisherFactory, times(1)).createCambriaPublisher(streamId);
        assertSame("should return same instance", firstPublisher, secondPublisher);
    }

    @Test
    public void getByStreamIdShouldHandleErrors() throws MalformedURLException, GeneralSecurityException {
        // given
        MalformedURLException exception = new MalformedURLException();
        given(publisherFactory.createCambriaPublisher(anyString())).willThrow(exception);
        expectedException.expect(allOf(
                instanceOf(UncheckedExecutionException.class),
                causeIsInstanceOf(exception.getClass())));

        // when
        cut.getByStreamId("a stream");

        // then
        // exception should have been thrown
    }

    @Test
    public void closeByStreamIdShouldCloseConnection() throws IOException, InterruptedException {
        // given
        String streamId = "sampleStream";
        given(cambriaPublisher.close(anyLong(), any(TimeUnit.class)))
                .willReturn(ImmutableList.of(new CambriaPublisher.message("p", "msg")));

        // when
        CambriaBatchingPublisher cachedPublisher = cut.getByStreamId(streamId);
        cut.closeByStreamId(streamId);

        // then
        assertSame("should return proper publisher", cambriaPublisher, cachedPublisher);
        verify(cambriaPublisher).close(20, TimeUnit.SECONDS);
    }

    @Test
    public void closeByStreamIdShouldHandleErrors() throws IOException, InterruptedException {
        // given
        String streamId = "sampleStream";
        given(cambriaPublisher.close(anyLong(), any(TimeUnit.class))).willThrow(IOException.class);

        // when
        CambriaBatchingPublisher cachedPublisher = cut.getByStreamId(streamId);
        cut.closeByStreamId(streamId);

        // then
        assertSame("should return proper publisher", cambriaPublisher, cachedPublisher);
        verify(cambriaPublisher).close(20, TimeUnit.SECONDS);
    }

    private Matcher<Exception> causeIsInstanceOf(final Class<?> clazz) {
        return new BaseMatcher<Exception>() {
            @Override
            public boolean matches(Object o) {
                return o instanceof Throwable && clazz.isInstance(((Throwable) o).getCause());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("exception cause should be an instance of " + clazz.getName());
            }
        };
    }
}