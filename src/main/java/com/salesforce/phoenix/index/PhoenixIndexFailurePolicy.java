/*******************************************************************************
 * Copyright (c) 2013, Salesforce.com, Inc. All rights reserved. Redistribution and use in source and binary forms, with
 * or without modification, are permitted provided that the following conditions are met: Redistributions of source code
 * must retain the above copyright notice, this list of conditions and the following disclaimer. Redistributions in
 * binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution. Neither the name of Salesforce.com nor the names
 * of its contributors may be used to endorse or promote products derived from this software without specific prior
 * written permission. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package com.salesforce.phoenix.index;

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.Stoppable;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;

import com.google.common.collect.Multimap;
import com.salesforce.hbase.index.table.HTableInterfaceReference;
import com.salesforce.hbase.index.write.KillServerOnFailurePolicy;
import com.salesforce.phoenix.coprocessor.MetaDataProtocol;
import com.salesforce.phoenix.coprocessor.MetaDataProtocol.MetaDataMutationResult;
import com.salesforce.phoenix.coprocessor.MetaDataProtocol.MutationCode;
import com.salesforce.phoenix.jdbc.PhoenixDatabaseMetaData;
import com.salesforce.phoenix.schema.PIndexState;
import com.salesforce.phoenix.util.SchemaUtil;

/**
 * 
 * Handler called in the event that index updates cannot be written to their
 * region server. First attempts to disable the index and failing that falls
 * back to the default behavior of killing the region server.
 *
 * @author jtaylor
 * @since 2.1
 */
public class PhoenixIndexFailurePolicy extends  KillServerOnFailurePolicy {
    private static final Log LOG = LogFactory.getLog(PhoenixIndexFailurePolicy.class);
    private RegionCoprocessorEnvironment env;

    public PhoenixIndexFailurePolicy() {
    }

    @Override
    public void setup(Stoppable parent, RegionCoprocessorEnvironment env) {
      super.setup(parent, env);
      this.env = env;
    }

    @Override
    public void handleFailure(Multimap<HTableInterfaceReference, Mutation> attempted, Exception cause) {
        try {
            for (HTableInterfaceReference ref : attempted.asMap().keySet()) {
                String indexTableName = ref.getTableName();
                byte[] indexTableKey = SchemaUtil.getTableKeyFromFullName(indexTableName);
                HTableInterface systemTable = env.getTable(PhoenixDatabaseMetaData.TYPE_TABLE_NAME_BYTES);
                MetaDataProtocol mdProxy = systemTable.coprocessorProxy(MetaDataProtocol.class, indexTableKey);
                Put put = new Put(indexTableKey);
                put.add(PhoenixDatabaseMetaData.TABLE_FAMILY_BYTES, PhoenixDatabaseMetaData.INDEX_STATE_BYTES, PIndexState.DISABLE.getSerializedBytes());
                List<Mutation> tableMetadata = Collections.<Mutation>singletonList(put);
                MetaDataMutationResult result = mdProxy.updateIndexState(tableMetadata);
                if (result.getMutationCode() != MutationCode.TABLE_ALREADY_EXISTS) {
                    LOG.warn("Attempt to disable index " + indexTableName + " failed with code = " + result.getMutationCode() + ". Will use default failure policy instead.");
                    super.handleFailure(attempted, cause);
                }
                // TODO: we should be able to communicate back to the indexing framework
                // that we don't want to process any more index updates for this index.
                LOG.info("Successfully disabled index " + indexTableName);
            }
        } catch (Throwable t) {
            super.handleFailure(attempted, cause);
        }
    }

}
