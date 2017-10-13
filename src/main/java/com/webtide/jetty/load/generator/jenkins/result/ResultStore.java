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

package com.webtide.jetty.load.generator.jenkins.result;

import org.mortbay.jetty.load.generator.listeners.LoadResult;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public interface ResultStore
{

    ExtendedLoadResult save( LoadResult loadResult );

    void remove( ExtendedLoadResult loadResult );

    List<ExtendedLoadResult> find( QueryFiler queryFiler );

    List<ExtendedLoadResult> findAll();

    String getProviderId();

    class QueryFiler
    {
        private String jettyVersion, uuid;

        private Date startDate, endDate;

        public String getJettyVersion()
        {
            return jettyVersion;
        }

        public void setJettyVersion( String jettyVersion )
        {
            this.jettyVersion = jettyVersion;
        }

        public QueryFiler jettyVersion( String jettyVersion )
        {
            this.jettyVersion = jettyVersion;
            return this;
        }

        public Date getStartDate()
        {
            return startDate;
        }

        public void setStartDate( Date startDate )
        {
            this.startDate = startDate;
        }

        public QueryFiler startDate( Date startDate )
        {
            this.startDate = startDate;
            return this;
        }

        public Date getEndDate()
        {
            return endDate;
        }

        public void setEndDate( Date endDate )
        {
            this.endDate = endDate;
        }

        public QueryFiler endDate( Date endDate )
        {
            this.endDate = endDate;
            return this;
        }

        public String getUuid()
        {
            return uuid;
        }

        public void setUuid( String uuid )
        {
            this.uuid = uuid;
        }

        public QueryFiler uuid( String uuid )
        {
            this.uuid = uuid;
            return this;
        }
    }


    class ExtendedLoadResult
        extends LoadResult
        implements Serializable
    {

        private String uuid;

        public ExtendedLoadResult()
        {
            // no op
        }

        public ExtendedLoadResult( String uuid )
        {
            this.uuid = uuid;
        }

        public ExtendedLoadResult( String uuid, LoadResult loadResult )
        {
            super( loadResult.getServerInfo(), loadResult.getCollectorInformations() );
            this.uuid = uuid;
        }

        public String getUuid()
        {
            return uuid;
        }

        @Override
        public String toString()
        {
            return "ExtendedLoadResult{" + "uuid='" + uuid + '\'' + '}' + super.toString();
        }
    }

}
