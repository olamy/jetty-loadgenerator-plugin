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
    "tasks",
    "queueSizeMax",
    "activeThreadsMax",
    "queueLatencyAverage",
    "queueLatencyMax",
    "taskTimeAverage",
    "taskTimeMax"
})
public class ThreadPool {

    @JsonProperty("tasks")
    private long tasks;
    @JsonProperty("queueSizeMax")
    private long queueSizeMax;
    @JsonProperty("activeThreadsMax")
    private long activeThreadsMax;
    @JsonProperty("queueLatencyAverage")
    private QueueLatencyAverage queueLatencyAverage;
    @JsonProperty("queueLatencyMax")
    private QueueLatencyMax queueLatencyMax;
    @JsonProperty("taskTimeAverage")
    private TaskTimeAverage taskTimeAverage;
    @JsonProperty("taskTimeMax")
    private TaskTimeMax taskTimeMax;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public ThreadPool() {
    }

    /**
     * 
     * @param queueLatencyAverage
     * @param queueLatencyMax
     * @param taskTimeMax
     * @param activeThreadsMax
     * @param tasks
     * @param taskTimeAverage
     * @param queueSizeMax
     */
    public ThreadPool(long tasks, long queueSizeMax, long activeThreadsMax, QueueLatencyAverage queueLatencyAverage, QueueLatencyMax queueLatencyMax, TaskTimeAverage taskTimeAverage, TaskTimeMax taskTimeMax) {
        this.tasks = tasks;
        this.queueSizeMax = queueSizeMax;
        this.activeThreadsMax = activeThreadsMax;
        this.queueLatencyAverage = queueLatencyAverage;
        this.queueLatencyMax = queueLatencyMax;
        this.taskTimeAverage = taskTimeAverage;
        this.taskTimeMax = taskTimeMax;
    }

    /**
     * 
     * @return
     *     The tasks
     */
    @JsonProperty("tasks")
    public long getTasks() {
        return tasks;
    }

    /**
     * 
     * @param tasks
     *     The tasks
     */
    @JsonProperty("tasks")
    public void setTasks(long tasks) {
        this.tasks = tasks;
    }

    /**
     * 
     * @return
     *     The queueSizeMax
     */
    @JsonProperty("queueSizeMax")
    public long getQueueSizeMax() {
        return queueSizeMax;
    }

    /**
     * 
     * @param queueSizeMax
     *     The queueSizeMax
     */
    @JsonProperty("queueSizeMax")
    public void setQueueSizeMax(long queueSizeMax) {
        this.queueSizeMax = queueSizeMax;
    }

    /**
     * 
     * @return
     *     The activeThreadsMax
     */
    @JsonProperty("activeThreadsMax")
    public long getActiveThreadsMax() {
        return activeThreadsMax;
    }

    /**
     * 
     * @param activeThreadsMax
     *     The activeThreadsMax
     */
    @JsonProperty("activeThreadsMax")
    public void setActiveThreadsMax(long activeThreadsMax) {
        this.activeThreadsMax = activeThreadsMax;
    }

    /**
     * 
     * @return
     *     The queueLatencyAverage
     */
    @JsonProperty("queueLatencyAverage")
    public QueueLatencyAverage getQueueLatencyAverage() {
        return queueLatencyAverage;
    }

    /**
     * 
     * @param queueLatencyAverage
     *     The queueLatencyAverage
     */
    @JsonProperty("queueLatencyAverage")
    public void setQueueLatencyAverage(QueueLatencyAverage queueLatencyAverage) {
        this.queueLatencyAverage = queueLatencyAverage;
    }

    /**
     * 
     * @return
     *     The queueLatencyMax
     */
    @JsonProperty("queueLatencyMax")
    public QueueLatencyMax getQueueLatencyMax() {
        return queueLatencyMax;
    }

    /**
     * 
     * @param queueLatencyMax
     *     The queueLatencyMax
     */
    @JsonProperty("queueLatencyMax")
    public void setQueueLatencyMax(QueueLatencyMax queueLatencyMax) {
        this.queueLatencyMax = queueLatencyMax;
    }

    /**
     * 
     * @return
     *     The taskTimeAverage
     */
    @JsonProperty("taskTimeAverage")
    public TaskTimeAverage getTaskTimeAverage() {
        return taskTimeAverage;
    }

    /**
     * 
     * @param taskTimeAverage
     *     The taskTimeAverage
     */
    @JsonProperty("taskTimeAverage")
    public void setTaskTimeAverage(TaskTimeAverage taskTimeAverage) {
        this.taskTimeAverage = taskTimeAverage;
    }

    /**
     * 
     * @return
     *     The taskTimeMax
     */
    @JsonProperty("taskTimeMax")
    public TaskTimeMax getTaskTimeMax() {
        return taskTimeMax;
    }

    /**
     * 
     * @param taskTimeMax
     *     The taskTimeMax
     */
    @JsonProperty("taskTimeMax")
    public void setTaskTimeMax(TaskTimeMax taskTimeMax) {
        this.taskTimeMax = taskTimeMax;
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
