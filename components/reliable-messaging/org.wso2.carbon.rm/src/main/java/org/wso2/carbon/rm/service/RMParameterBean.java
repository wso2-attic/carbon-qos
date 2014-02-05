/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
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
 */
package org.wso2.carbon.rm.service;


public class RMParameterBean {

    private long inactiveTimeoutValue;

    private String inactivityTimeoutMeasure;

    private long inactivityTimeoutInterval;

    private long sequenceRemovalTimeoutValue;

    private String sequenceRemovalTimeoutMeasure;

    private long sequenceRemovalTimeoutInterval;

    private long acknowledgementInterval;

    private long retransmissionInterval;

    private boolean exponentialBackoff;

    private int maximumRetransmissionCount;

    public long getInactiveTimeoutValue() {
        return inactiveTimeoutValue;
    }

    public void setInactiveTimeoutValue(long inactiveTimeoutValue) {
        this.inactiveTimeoutValue = inactiveTimeoutValue;
    }

    public String getInactivityTimeoutMeasure() {
        return inactivityTimeoutMeasure;
    }

    public void setInactivityTimeoutMeasure(String inactivityTimeoutMeasure) {
        this.inactivityTimeoutMeasure = inactivityTimeoutMeasure;
    }

    public long getInactivityTimeoutInterval() {
        return inactivityTimeoutInterval;
    }

    public void setInactivityTimeoutInterval(long inactivityTimeoutInterval) {
        this.inactivityTimeoutInterval = inactivityTimeoutInterval;
    }

    public long getSequenceRemovalTimeoutValue() {
        return sequenceRemovalTimeoutValue;
    }

    public void setSequenceRemovalTimeoutValue(long sequenceRemovalTimeoutValue) {
        this.sequenceRemovalTimeoutValue = sequenceRemovalTimeoutValue;
    }

    public String getSequenceRemovalTimeoutMeasure() {
        return sequenceRemovalTimeoutMeasure;
    }

    public void setSequenceRemovalTimeoutMeasure(String sequenceRemovalTimeoutMeasure) {
        this.sequenceRemovalTimeoutMeasure = sequenceRemovalTimeoutMeasure;
    }

    public long getSequenceRemovalTimeoutInterval() {
        return sequenceRemovalTimeoutInterval;
    }

    public void setSequenceRemovalTimeoutInterval(long sequenceRemovalTimeoutInterval) {
        this.sequenceRemovalTimeoutInterval = sequenceRemovalTimeoutInterval;
    }

    public long getAcknowledgementInterval() {
        return acknowledgementInterval;
    }

    public void setAcknowledgementInterval(long acknowledgementInterval) {
        this.acknowledgementInterval = acknowledgementInterval;
    }

    public long getRetransmissionInterval() {
        return retransmissionInterval;
    }

    public void setRetransmissionInterval(long retransmissionInterval) {
        this.retransmissionInterval = retransmissionInterval;
    }

    public boolean isExponentialBackoff() {
        return exponentialBackoff;
    }

    public void setExponentialBackoff(boolean exponentialBackoff) {
        this.exponentialBackoff = exponentialBackoff;
    }

    public int getMaximumRetransmissionCount() {
        return maximumRetransmissionCount;
    }

    public void setMaximumRetransmissionCount(int maximumRetransmissionCount) {
        this.maximumRetransmissionCount = maximumRetransmissionCount;
    }
}
