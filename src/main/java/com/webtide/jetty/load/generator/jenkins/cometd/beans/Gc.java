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

package com.webtide.jetty.load.generator.jenkins.cometd.beans;

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
    "youngCount",
    "youngTime",
    "oldCount",
    "oldTime",
    "youngGarbage",
    "oldGarbage"
})
public class Gc {

    @JsonProperty("youngCount")
    private long youngCount;
    @JsonProperty("youngTime")
    private YoungTime youngTime;
    @JsonProperty("oldCount")
    private long oldCount;
    @JsonProperty("oldTime")
    private OldTime oldTime;
    @JsonProperty("youngGarbage")
    private YoungGarbage youngGarbage;
    @JsonProperty("oldGarbage")
    private OldGarbage oldGarbage;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public Gc() {
    }

    /**
     * 
     * @param oldCount
     * @param youngCount
     * @param youngGarbage
     * @param oldGarbage
     * @param oldTime
     * @param youngTime
     */
    public Gc(long youngCount, YoungTime youngTime, long oldCount, OldTime oldTime, YoungGarbage youngGarbage, OldGarbage oldGarbage) {
        this.youngCount = youngCount;
        this.youngTime = youngTime;
        this.oldCount = oldCount;
        this.oldTime = oldTime;
        this.youngGarbage = youngGarbage;
        this.oldGarbage = oldGarbage;
    }

    /**
     * 
     * @return
     *     The youngCount
     */
    @JsonProperty("youngCount")
    public long getYoungCount() {
        return youngCount;
    }

    /**
     * 
     * @param youngCount
     *     The youngCount
     */
    @JsonProperty("youngCount")
    public void setYoungCount(long youngCount) {
        this.youngCount = youngCount;
    }

    /**
     * 
     * @return
     *     The youngTime
     */
    @JsonProperty("youngTime")
    public YoungTime getYoungTime() {
        return youngTime;
    }

    /**
     * 
     * @param youngTime
     *     The youngTime
     */
    @JsonProperty("youngTime")
    public void setYoungTime(YoungTime youngTime) {
        this.youngTime = youngTime;
    }

    /**
     * 
     * @return
     *     The oldCount
     */
    @JsonProperty("oldCount")
    public long getOldCount() {
        return oldCount;
    }

    /**
     * 
     * @param oldCount
     *     The oldCount
     */
    @JsonProperty("oldCount")
    public void setOldCount(long oldCount) {
        this.oldCount = oldCount;
    }

    /**
     * 
     * @return
     *     The oldTime
     */
    @JsonProperty("oldTime")
    public OldTime getOldTime() {
        return oldTime;
    }

    /**
     * 
     * @param oldTime
     *     The oldTime
     */
    @JsonProperty("oldTime")
    public void setOldTime(OldTime oldTime) {
        this.oldTime = oldTime;
    }

    /**
     * 
     * @return
     *     The youngGarbage
     */
    @JsonProperty("youngGarbage")
    public YoungGarbage getYoungGarbage() {
        return youngGarbage;
    }

    /**
     * 
     * @param youngGarbage
     *     The youngGarbage
     */
    @JsonProperty("youngGarbage")
    public void setYoungGarbage(YoungGarbage youngGarbage) {
        this.youngGarbage = youngGarbage;
    }

    /**
     * 
     * @return
     *     The oldGarbage
     */
    @JsonProperty("oldGarbage")
    public OldGarbage getOldGarbage() {
        return oldGarbage;
    }

    /**
     * 
     * @param oldGarbage
     *     The oldGarbage
     */
    @JsonProperty("oldGarbage")
    public void setOldGarbage(OldGarbage oldGarbage) {
        this.oldGarbage = oldGarbage;
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
