/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.storemigration;

import org.junit.Test;
import org.neo4j.kernel.impl.nioneo.store.NodeRecord;

import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertEquals;

public class ReadRecordsTest
{
    @Test
    public void shouldReadNodeRecords() throws IOException
    {
        URL nodeStoreFile = getClass().getResource( "oldformatstore/neostore.nodestore.db" );

        Iterable<NodeRecord> records = new LegacyNodeStoreReader().readNodeStore( nodeStoreFile.getFile() );
        int nodeCount = 0;
        for ( NodeRecord record : records )
        {
            nodeCount++;
        }
        assertEquals( 1001, nodeCount );
    }

    @Test
    public void shouldReadPropertyRecords() throws IOException
    {
        URL propertyStoreFile = getClass().getResource( "oldformatstore/neostore.propertystore.db" );

        Iterable<LegacyPropertyStoreReader.LegacyPropertyRecord> records = new LegacyPropertyStoreReader().readPropertyStore( propertyStoreFile.getFile() );
        int propertyCount = 0;
        for ( LegacyPropertyStoreReader.LegacyPropertyRecord record : records )
        {
            int keyIndexId = record.getKeyIndexId();
            assertEquals( 0, keyIndexId );
            Object value = record.getType().getValue( record, null );
            assertEquals( true, value );
            propertyCount++;
        }
        assertEquals( 1000, propertyCount );
    }
}
