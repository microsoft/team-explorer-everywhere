// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.flags.ScheduleDays2010;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._Schedule;

public class Schedule2010 extends WebServiceObjectWrapper {
    private Schedule2010() {
        this(new _Schedule());
    }

    public Schedule2010(final _Schedule value) {
        super(value);
    }

    public Schedule2010(final Schedule schedule) {
        this();

        getWebServiceObject().setTimeZoneId(schedule.getWebServiceObject().getTimeZoneId());
        setUtcDaysToBuild(TFS2010Helper.convert(schedule.getDaysToBuild()));
        getWebServiceObject().setUtcStartTime(schedule.getWebServiceObject().getUtcStartTime());
    }

    public _Schedule getWebServiceObject() {
        return (_Schedule) webServiceObject;
    }

    public ScheduleDays2010 getUtcDaysToBuild() {
        return new ScheduleDays2010(getWebServiceObject().getUtcDaysToBuild());
    }

    public int getUtcStartTime() {
        return getWebServiceObject().getUtcStartTime();
    }

    public void setUtcDaysToBuild(final ScheduleDays2010 value) {
        getWebServiceObject().setUtcDaysToBuild(value.getWebServiceObject());
    }
}
