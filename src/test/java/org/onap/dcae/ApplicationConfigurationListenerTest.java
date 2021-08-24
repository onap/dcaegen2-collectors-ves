/*-
 * ============LICENSE_START=======================================================
 * VES Collector
 * ================================================================================
 * Copyright (C) 2020-2021 Nokia. All rights reserved.
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
package org.onap.dcae;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.dcae.configuration.ConfigurationHandler;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationConfigurationListenerTest {

    @Mock
    private ConfigurationHandler configurationHandler;

    @InjectMocks
    @Spy
    private ApplicationConfigurationListener applicationConfigurationListener;

    @Test
    public void shouldStopJobAndCloseConnectionWhenErrorOccurredDuringListenAtConfigChange() {

       // given
       Mockito.doThrow(new RuntimeException("Simulate exception")).when(configurationHandler).startListen(any());

       // when
       applicationConfigurationListener.run();

       // then
        Mockito.verify(applicationConfigurationListener).stopListeningForConfigurationUpdates(any());
    }

    @Test
    public void shouldSendReloadAction() {

        // when
        applicationConfigurationListener.reload(Duration.ofMillis(1));

        // then
        Mockito.verify(applicationConfigurationListener).sendReloadAction();
    }
}
