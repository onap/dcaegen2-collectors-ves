package org.onap.dcae.common;

import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;


public class HeaderUtilsTest {

    private HeaderUtils headerUtils = new HeaderUtils();

    @Test
    public void shouldReturnEventListenerRestApiIdentifier() {
        assertThat(
                headerUtils.getRestApiIdentify("http://localhost/eventListener/v2/eventListener")
        ).isEqualTo("eventListener");
    }

    @Test
    public void shouldReturnBatchEventRestApiIdentifier() {
        assertThat(
                headerUtils.getRestApiIdentify("http://localhost/eventListener/v2/eventBatch")
        ).isEqualTo("eventListener_eventBatch");
    }

    @Test
    public void shouldExtractHeadersFromRequest() {
        // given
        final MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.addHeader("first", 1);
        httpServletRequest.addHeader("second", 2);

        // when/then
        assertThat(
                headerUtils.extractHeaders(httpServletRequest)
        ).contains(
                entry("first", "1"),
                entry("second", "2")
        );
    }

    @Test
    public void shouldCreateHttpHeaderWithSelectedData() {
        // given
        Map<String, String> data = Map.of("first", "1", "second", "2");

        // when
        final HttpHeaders httpHeaders = headerUtils.fillHeaders(data);

        //then
        assertThatHeaderContainsElement(httpHeaders, "first", "1");
        assertThatHeaderContainsElement(httpHeaders, "second", "2");
    }

    private void assertThatHeaderContainsElement(HttpHeaders httpHeaders, String key, String value) {
        assertThat(httpHeaders.containsKey(key)).isTrue();
        assertThat(httpHeaders.get(key)).contains(value);
    }

}
