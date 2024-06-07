/*
 * Copyright (c) 2023 Alcosi Group Ltd. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alcosi.nft.apigateway.config.db.r2dbc;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;
@ConfigurationProperties(prefix = "spring.r2dbc")
public class R2DBCConnectionFactoryOptionsProperties  {
    private Boolean enabled = false;
    private Integer threads = 10;
    private Integer maxTasks = 10;
    private Integer requestHistoryFilterOrder = -2147483648;
    private String requestHistoryIpHeader = "x-real-ip";
    private Duration partitionsInitSchedulerDelay = Duration.ofDays(1);
    private Integer partitionsInitMonthDelta = 2;

    private List<String > requestHistoryMaskHeaders = List.of("AUTHORIZATION","ValidationToken");

    public List<String> getRequestHistoryMaskHeaders() {
        return requestHistoryMaskHeaders;
    }

    public void setRequestHistoryMaskHeaders(List<String> requestHistoryMaskHeaders) {
        this.requestHistoryMaskHeaders = requestHistoryMaskHeaders;
    }

    public Duration getPartitionsInitSchedulerDelay() {
        return partitionsInitSchedulerDelay;
    }

    public void setPartitionsInitSchedulerDelay(Duration partitionsInitSchedulerDelay) {
        this.partitionsInitSchedulerDelay = partitionsInitSchedulerDelay;
    }

    public Integer getPartitionsInitMonthDelta() {
        return partitionsInitMonthDelta;
    }

    public void setPartitionsInitMonthDelta(Integer partitionsInitMonthDelta) {
        this.partitionsInitMonthDelta = partitionsInitMonthDelta;
    }

    public String getRequestHistoryIpHeader() {
        return requestHistoryIpHeader;
    }

    public void setRequestHistoryIpHeader(String requestHistoryIpHeader) {
        this.requestHistoryIpHeader = requestHistoryIpHeader;
    }

    public Integer getThreads() {
        return threads;
    }

    public void setThreads(Integer threads) {
        this.threads = threads;
    }

    public Integer getMaxTasks() {
        return maxTasks;
    }

    public void setMaxTasks(Integer maxTasks) {
        this.maxTasks = maxTasks;
    }

    public Integer getRequestHistoryFilterOrder() {
        return requestHistoryFilterOrder;
    }

    public void setRequestHistoryFilterOrder(Integer requestHistoryFilterOrder) {
        this.requestHistoryFilterOrder = requestHistoryFilterOrder;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    private Map<String,String> options= Map.of();

    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options;
    }
}