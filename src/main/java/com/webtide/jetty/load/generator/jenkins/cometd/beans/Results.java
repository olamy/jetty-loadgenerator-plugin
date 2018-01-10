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
    "cpu",
    "jitTime",
    "messages",
    "sendTime",
    "sendRate",
    "receiveTime",
    "receiveRate",
    "latency",
    "threadPool",
    "gc"
})
public class Results {

    @JsonProperty("cpu")
    private Cpu cpu;
    @JsonProperty("jitTime")
    private JitTime jitTime;
    @JsonProperty("messages")
    private long messages;
    @JsonProperty("sendTime")
    private SendTime sendTime;
    @JsonProperty("sendRate")
    private SendRate sendRate;
    @JsonProperty("receiveTime")
    private ReceiveTime receiveTime;
    @JsonProperty("receiveRate")
    private ReceiveRate receiveRate;
    @JsonProperty("latency")
    private Latency latency;
    @JsonProperty("threadPool")
    private ThreadPool threadPool;
    @JsonProperty("gc")
    private Gc gc;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public Results() {
    }

    /**
     * 
     * @param latency
     * @param threadPool
     * @param sendRate
     * @param receiveRate
     * @param jitTime
     * @param gc
     * @param receiveTime
     * @param sendTime
     * @param cpu
     * @param messages
     */
    public Results(Cpu cpu, JitTime jitTime, long messages, SendTime sendTime, SendRate sendRate, ReceiveTime receiveTime, ReceiveRate receiveRate, Latency latency, ThreadPool threadPool, Gc gc) {
        this.cpu = cpu;
        this.jitTime = jitTime;
        this.messages = messages;
        this.sendTime = sendTime;
        this.sendRate = sendRate;
        this.receiveTime = receiveTime;
        this.receiveRate = receiveRate;
        this.latency = latency;
        this.threadPool = threadPool;
        this.gc = gc;
    }

    /**
     * 
     * @return
     *     The cpu
     */
    @JsonProperty("cpu")
    public Cpu getCpu() {
        return cpu;
    }

    /**
     * 
     * @param cpu
     *     The cpu
     */
    @JsonProperty("cpu")
    public void setCpu(Cpu cpu) {
        this.cpu = cpu;
    }

    /**
     * 
     * @return
     *     The jitTime
     */
    @JsonProperty("jitTime")
    public JitTime getJitTime() {
        return jitTime;
    }

    /**
     * 
     * @param jitTime
     *     The jitTime
     */
    @JsonProperty("jitTime")
    public void setJitTime(JitTime jitTime) {
        this.jitTime = jitTime;
    }

    /**
     * 
     * @return
     *     The messages
     */
    @JsonProperty("messages")
    public long getMessages() {
        return messages;
    }

    /**
     * 
     * @param messages
     *     The messages
     */
    @JsonProperty("messages")
    public void setMessages(long messages) {
        this.messages = messages;
    }

    /**
     * 
     * @return
     *     The sendTime
     */
    @JsonProperty("sendTime")
    public SendTime getSendTime() {
        return sendTime;
    }

    /**
     * 
     * @param sendTime
     *     The sendTime
     */
    @JsonProperty("sendTime")
    public void setSendTime(SendTime sendTime) {
        this.sendTime = sendTime;
    }

    /**
     * 
     * @return
     *     The sendRate
     */
    @JsonProperty("sendRate")
    public SendRate getSendRate() {
        return sendRate;
    }

    /**
     * 
     * @param sendRate
     *     The sendRate
     */
    @JsonProperty("sendRate")
    public void setSendRate(SendRate sendRate) {
        this.sendRate = sendRate;
    }

    /**
     * 
     * @return
     *     The receiveTime
     */
    @JsonProperty("receiveTime")
    public ReceiveTime getReceiveTime() {
        return receiveTime;
    }

    /**
     * 
     * @param receiveTime
     *     The receiveTime
     */
    @JsonProperty("receiveTime")
    public void setReceiveTime(ReceiveTime receiveTime) {
        this.receiveTime = receiveTime;
    }

    /**
     * 
     * @return
     *     The receiveRate
     */
    @JsonProperty("receiveRate")
    public ReceiveRate getReceiveRate() {
        return receiveRate;
    }

    /**
     * 
     * @param receiveRate
     *     The receiveRate
     */
    @JsonProperty("receiveRate")
    public void setReceiveRate(ReceiveRate receiveRate) {
        this.receiveRate = receiveRate;
    }

    /**
     * 
     * @return
     *     The latency
     */
    @JsonProperty("latency")
    public Latency getLatency() {
        return latency;
    }

    /**
     * 
     * @param latency
     *     The latency
     */
    @JsonProperty("latency")
    public void setLatency(Latency latency) {
        this.latency = latency;
    }

    /**
     * 
     * @return
     *     The threadPool
     */
    @JsonProperty("threadPool")
    public ThreadPool getThreadPool() {
        return threadPool;
    }

    /**
     * 
     * @param threadPool
     *     The threadPool
     */
    @JsonProperty("threadPool")
    public void setThreadPool(ThreadPool threadPool) {
        this.threadPool = threadPool;
    }

    /**
     * 
     * @return
     *     The gc
     */
    @JsonProperty("gc")
    public Gc getGc() {
        return gc;
    }

    /**
     * 
     * @param gc
     *     The gc
     */
    @JsonProperty("gc")
    public void setGc(Gc gc) {
        this.gc = gc;
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
