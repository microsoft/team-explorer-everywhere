// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

 /*
* ---------------------------------------------------------
* Generated file, DO NOT EDIT
* ---------------------------------------------------------
*
* See following wiki page for instructions on how to regenerate:
*   https://vsowiki.com/index.php?title=Rest_Client_Generation
*/

package com.microsoft.teamfoundation.distributedtask.webapi;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.core.type.TypeReference;
import com.microsoft.teamfoundation.distributedtask.webapi.model.JobEvent;
import com.microsoft.teamfoundation.distributedtask.webapi.model.TaskLog;
import com.microsoft.teamfoundation.distributedtask.webapi.model.TaskOrchestrationPlan;
import com.microsoft.teamfoundation.distributedtask.webapi.model.Timeline;
import com.microsoft.teamfoundation.distributedtask.webapi.model.TimelineRecord;
import com.microsoft.visualstudio.services.webapi.model.VssJsonCollectionWrapper;
import com.microsoft.vss.client.core.model.ApiResourceVersion;
import com.microsoft.vss.client.core.model.NameValueCollection;
import com.microsoft.vss.client.core.VssHttpClientBase;

public abstract class TaskHttpClientBase
    extends VssHttpClientBase {

    private final static Map<String, Class<? extends Exception>> TRANSLATED_EXCEPTIONS;

    static {
        TRANSLATED_EXCEPTIONS = new HashMap<String, Class<? extends Exception>>();
    }

    /**
    * Create a new instance of TaskHttpClientBase
    *
    * @param jaxrsClient
    *            an initialized instance of a JAX-RS Client implementation
    * @param baseUrl
    *            a TFS project collection URL
    */
    public TaskHttpClientBase(final Object jaxrsClient, final URI baseUrl) {
        super(jaxrsClient, baseUrl);
    }

    /**
    * Create a new instance of TaskHttpClientBase
    *
    * @param tfsConnection
    *            an initialized instance of a TfsTeamProjectCollection
    */
    public TaskHttpClientBase(final Object tfsConnection) {
        super(tfsConnection);
    }

    @Override
    protected Map<String, Class<? extends Exception>> getTranslatedExceptions() {
        return TRANSLATED_EXCEPTIONS;
    }

    /** 
     * @param eventData 
     *            
     * @param planId 
     *            
     */
    public void postEvent(
        final JobEvent eventData, 
        final UUID planId) {

        final UUID locationId = UUID.fromString("557624af-b29e-4c20-8ab0-0399d2204f3f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       eventData,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest);
    }

    /** 
     * @param lines 
     *            
     * @param planId 
     *            
     * @param timelineId 
     *            
     * @param recordId 
     *            
     */
    public void postLines(
        final VssJsonCollectionWrapper<List<String>> lines, 
        final UUID planId, 
        final UUID timelineId, 
        final UUID recordId) {

        final UUID locationId = UUID.fromString("858983e4-19bd-4c5e-864c-507b59b58b12"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$
        routeValues.put("timelineId", timelineId); //$NON-NLS-1$
        routeValues.put("recordId", recordId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       lines,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest);
    }

    /** 
     * @param planId 
     *            
     * @param logId 
     *            
     * @return TaskLog
     */
    public TaskLog appendLog(
        final InputStream uploadStream,
        final UUID planId, 
        final int logId) {

        final UUID locationId = UUID.fromString("46f5667d-263a-4684-91b1-dff7fdcf64e2"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$
        routeValues.put("logId", logId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       uploadStream,
                                                       APPLICATION_OCTET_STREAM_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, TaskLog.class);
    }

    /** 
     * @param log 
     *            
     * @param planId 
     *            
     * @return TaskLog
     */
    public TaskLog createLog(
        final TaskLog log, 
        final UUID planId) {

        final UUID locationId = UUID.fromString("46f5667d-263a-4684-91b1-dff7fdcf64e2"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       log,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, TaskLog.class);
    }

    /** 
     * @param planId 
     *            
     * @param logId 
     *            
     * @param startLine 
     *            
     * @param endLine 
     *            
     * @return List<String>
     */
    public List<String> getLog(
        final UUID planId, 
        final int logId, 
        final Integer startLine, 
        final Integer endLine) {

        final UUID locationId = UUID.fromString("46f5667d-263a-4684-91b1-dff7fdcf64e2"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$
        routeValues.put("logId", logId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("startLine", startLine); //$NON-NLS-1$
        queryParameters.addIfNotNull("endLine", endLine); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<String>>() {});
    }

    /** 
     * @param planId 
     *            
     * @return List<TaskLog>
     */
    public List<TaskLog> getLogs(
        final UUID planId) {

        final UUID locationId = UUID.fromString("46f5667d-263a-4684-91b1-dff7fdcf64e2"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<TaskLog>>() {});
    }

    /** 
     * @param planId 
     *            
     * @return TaskOrchestrationPlan
     */
    public TaskOrchestrationPlan getPlan(
        final UUID planId) {

        final UUID locationId = UUID.fromString("5cecd946-d704-471e-a45f-3b4064fcfaba"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, TaskOrchestrationPlan.class);
    }

    /** 
     * @param planId 
     *            
     * @param timelineId 
     *            
     * @param changeId 
     *            
     * @return List<TimelineRecord>
     */
    public List<TimelineRecord> getRecords(
        final UUID planId, 
        final UUID timelineId, 
        final Integer changeId) {

        final UUID locationId = UUID.fromString("8893bc5b-35b2-4be7-83cb-99e683551db4"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$
        routeValues.put("timelineId", timelineId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("changeId", changeId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<TimelineRecord>>() {});
    }

    /** 
     * @param records 
     *            
     * @param planId 
     *            
     * @param timelineId 
     *            
     * @return List<TimelineRecord>
     */
    public List<TimelineRecord> updateRecords(
        final VssJsonCollectionWrapper<List<TimelineRecord>> records, 
        final UUID planId, 
        final UUID timelineId) {

        final UUID locationId = UUID.fromString("8893bc5b-35b2-4be7-83cb-99e683551db4"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$
        routeValues.put("timelineId", timelineId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PATCH,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       records,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<TimelineRecord>>() {});
    }

    /** 
     * @param timeline 
     *            
     * @param planId 
     *            
     * @return Timeline
     */
    public Timeline createTimeline(
        final Timeline timeline, 
        final UUID planId) {

        final UUID locationId = UUID.fromString("83597576-cc2c-453c-bea6-2882ae6a1653"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       timeline,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, Timeline.class);
    }

    /** 
     * @param planId 
     *            
     * @param timelineId 
     *            
     */
    public void deleteTimeline(
        final UUID planId, 
        final UUID timelineId) {

        final UUID locationId = UUID.fromString("83597576-cc2c-453c-bea6-2882ae6a1653"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$
        routeValues.put("timelineId", timelineId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.DELETE,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest);
    }

    /** 
     * @param planId 
     *            
     * @param timelineId 
     *            
     * @param changeId 
     *            
     * @param includeRecords 
     *            
     * @return Timeline
     */
    public Timeline getTimeline(
        final UUID planId, 
        final UUID timelineId, 
        final Integer changeId, 
        final Boolean includeRecords) {

        final UUID locationId = UUID.fromString("83597576-cc2c-453c-bea6-2882ae6a1653"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$
        routeValues.put("timelineId", timelineId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("changeId", changeId); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeRecords", includeRecords); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, Timeline.class);
    }

    /** 
     * @param planId 
     *            
     * @return List<Timeline>
     */
    public List<Timeline> getTimelines(
        final UUID planId) {

        final UUID locationId = UUID.fromString("83597576-cc2c-453c-bea6-2882ae6a1653"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("planId", planId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<Timeline>>() {});
    }
}
