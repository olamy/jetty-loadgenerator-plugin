//
//  ========================================================================
//  Copyright (c) 1995-2016 Webtide LLC, Olivier Lamy
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
    "cores",
    "totalMemory",
    "os",
    "jvm",
    "totalHeap",
    "date",
    "transport",
    "clients",
    "rooms",
    "roomsPerClient",
    "batches",
    "batchSize",
    "batchPause",
    "messageSize"
})
public class Config {

    @JsonProperty("cores")
    private long cores;
    @JsonProperty("totalMemory")
    private TotalMemory totalMemory;
    @JsonProperty("os")
    private String os;
    @JsonProperty("jvm")
    private String jvm;
    @JsonProperty("totalHeap")
    private TotalHeap totalHeap;
    @JsonProperty("date")
    private String date;
    @JsonProperty("transport")
    private String transport;
    @JsonProperty("clients")
    private long clients;
    @JsonProperty("rooms")
    private long rooms;
    @JsonProperty("roomsPerClient")
    private long roomsPerClient;
    @JsonProperty("batches")
    private long batches;
    @JsonProperty("batchSize")
    private long batchSize;
    @JsonProperty("batchPause")
    private BatchPause batchPause;
    @JsonProperty("messageSize")
    private MessageSize messageSize;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public Config() {
    }

    /**
     * 
     * @param os
     * @param totalMemory
     * @param batchPause
     * @param batchSize
     * @param jvm
     * @param date
     * @param cores
     * @param totalHeap
     * @param transport
     * @param batches
     * @param roomsPerClient
     * @param messageSize
     * @param clients
     * @param rooms
     */
    public Config(long cores, TotalMemory totalMemory, String os, String jvm, TotalHeap totalHeap, String date, String transport, long clients, long rooms, long roomsPerClient, long batches, long batchSize, BatchPause batchPause, MessageSize messageSize) {
        this.cores = cores;
        this.totalMemory = totalMemory;
        this.os = os;
        this.jvm = jvm;
        this.totalHeap = totalHeap;
        this.date = date;
        this.transport = transport;
        this.clients = clients;
        this.rooms = rooms;
        this.roomsPerClient = roomsPerClient;
        this.batches = batches;
        this.batchSize = batchSize;
        this.batchPause = batchPause;
        this.messageSize = messageSize;
    }

    /**
     * 
     * @return
     *     The cores
     */
    @JsonProperty("cores")
    public long getCores() {
        return cores;
    }

    /**
     * 
     * @param cores
     *     The cores
     */
    @JsonProperty("cores")
    public void setCores(long cores) {
        this.cores = cores;
    }

    /**
     * 
     * @return
     *     The totalMemory
     */
    @JsonProperty("totalMemory")
    public TotalMemory getTotalMemory() {
        return totalMemory;
    }

    /**
     * 
     * @param totalMemory
     *     The totalMemory
     */
    @JsonProperty("totalMemory")
    public void setTotalMemory(TotalMemory totalMemory) {
        this.totalMemory = totalMemory;
    }

    /**
     * 
     * @return
     *     The os
     */
    @JsonProperty("os")
    public String getOs() {
        return os;
    }

    /**
     * 
     * @param os
     *     The os
     */
    @JsonProperty("os")
    public void setOs(String os) {
        this.os = os;
    }

    /**
     * 
     * @return
     *     The jvm
     */
    @JsonProperty("jvm")
    public String getJvm() {
        return jvm;
    }

    /**
     * 
     * @param jvm
     *     The jvm
     */
    @JsonProperty("jvm")
    public void setJvm(String jvm) {
        this.jvm = jvm;
    }

    /**
     * 
     * @return
     *     The totalHeap
     */
    @JsonProperty("totalHeap")
    public TotalHeap getTotalHeap() {
        return totalHeap;
    }

    /**
     * 
     * @param totalHeap
     *     The totalHeap
     */
    @JsonProperty("totalHeap")
    public void setTotalHeap(TotalHeap totalHeap) {
        this.totalHeap = totalHeap;
    }

    /**
     * 
     * @return
     *     The date
     */
    @JsonProperty("date")
    public String getDate() {
        return date;
    }

    /**
     * 
     * @param date
     *     The date
     */
    @JsonProperty("date")
    public void setDate(String date) {
        this.date = date;
    }

    /**
     * 
     * @return
     *     The transport
     */
    @JsonProperty("transport")
    public String getTransport() {
        return transport;
    }

    /**
     * 
     * @param transport
     *     The transport
     */
    @JsonProperty("transport")
    public void setTransport(String transport) {
        this.transport = transport;
    }

    /**
     * 
     * @return
     *     The clients
     */
    @JsonProperty("clients")
    public long getClients() {
        return clients;
    }

    /**
     * 
     * @param clients
     *     The clients
     */
    @JsonProperty("clients")
    public void setClients(long clients) {
        this.clients = clients;
    }

    /**
     * 
     * @return
     *     The rooms
     */
    @JsonProperty("rooms")
    public long getRooms() {
        return rooms;
    }

    /**
     * 
     * @param rooms
     *     The rooms
     */
    @JsonProperty("rooms")
    public void setRooms(long rooms) {
        this.rooms = rooms;
    }

    /**
     * 
     * @return
     *     The roomsPerClient
     */
    @JsonProperty("roomsPerClient")
    public long getRoomsPerClient() {
        return roomsPerClient;
    }

    /**
     * 
     * @param roomsPerClient
     *     The roomsPerClient
     */
    @JsonProperty("roomsPerClient")
    public void setRoomsPerClient(long roomsPerClient) {
        this.roomsPerClient = roomsPerClient;
    }

    /**
     * 
     * @return
     *     The batches
     */
    @JsonProperty("batches")
    public long getBatches() {
        return batches;
    }

    /**
     * 
     * @param batches
     *     The batches
     */
    @JsonProperty("batches")
    public void setBatches(long batches) {
        this.batches = batches;
    }

    /**
     * 
     * @return
     *     The batchSize
     */
    @JsonProperty("batchSize")
    public long getBatchSize() {
        return batchSize;
    }

    /**
     * 
     * @param batchSize
     *     The batchSize
     */
    @JsonProperty("batchSize")
    public void setBatchSize(long batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * 
     * @return
     *     The batchPause
     */
    @JsonProperty("batchPause")
    public BatchPause getBatchPause() {
        return batchPause;
    }

    /**
     * 
     * @param batchPause
     *     The batchPause
     */
    @JsonProperty("batchPause")
    public void setBatchPause(BatchPause batchPause) {
        this.batchPause = batchPause;
    }

    /**
     * 
     * @return
     *     The messageSize
     */
    @JsonProperty("messageSize")
    public MessageSize getMessageSize() {
        return messageSize;
    }

    /**
     * 
     * @param messageSize
     *     The messageSize
     */
    @JsonProperty("messageSize")
    public void setMessageSize(MessageSize messageSize) {
        this.messageSize = messageSize;
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
