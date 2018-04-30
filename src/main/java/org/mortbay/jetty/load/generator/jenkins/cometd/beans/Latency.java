//
//  ========================================================================
//  Copyright (c) 1995-2018 Webtide LLC, Olivier Lamy
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================

package org.mortbay.jetty.load.generator.jenkins.cometd.beans;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "min",
    "p50",
    "p99",
    "max"
})
public class Latency {

    @JsonProperty("min")
    private Min min;
    @JsonProperty("p50")
    private P50 p50;
    @JsonProperty("p99")
    private P99 p99;
    @JsonProperty("max")
    private Max max;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public Latency() {
    }

    /**
     * 
     * @param min
     * @param max
     * @param p99
     * @param p50
     */
    public Latency(Min min, P50 p50, P99 p99, Max max) {
        this.min = min;
        this.p50 = p50;
        this.p99 = p99;
        this.max = max;
    }

    /**
     * 
     * @return
     *     The min
     */
    @JsonProperty("min")
    public Min getMin() {
        return min;
    }

    /**
     * 
     * @param min
     *     The min
     */
    @JsonProperty("min")
    public void setMin(Min min) {
        this.min = min;
    }

    /**
     * 
     * @return
     *     The p50
     */
    @JsonProperty("p50")
    public P50 getP50() {
        return p50;
    }

    /**
     * 
     * @param p50
     *     The p50
     */
    @JsonProperty("p50")
    public void setP50(P50 p50) {
        this.p50 = p50;
    }

    /**
     * 
     * @return
     *     The p99
     */
    @JsonProperty("p99")
    public P99 getP99() {
        return p99;
    }

    /**
     * 
     * @param p99
     *     The p99
     */
    @JsonProperty("p99")
    public void setP99(P99 p99) {
        this.p99 = p99;
    }

    /**
     * 
     * @return
     *     The max
     */
    @JsonProperty("max")
    public Max getMax() {
        return max;
    }

    /**
     * 
     * @param max
     *     The max
     */
    @JsonProperty("max")
    public void setMax(Max max) {
        this.max = max;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
