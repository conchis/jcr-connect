/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.northwestern.jcr.adapter.xtf.query;

import org.apache.jackrabbit.core.NodeId;

import java.io.IOException;

/**
 * Wraps the hits.
 */
public class XTFQueryHits extends AbstractQueryHits {

	/**
	 * The array of name
	 */
	private final String [] hits;

    /**
     * The index of the current hit. Initially invalid.
     */
    private int hitIndex = -1;

    /**
     * Creates a new <code>QueryHits</code> instance wrapping <code>hits</code>.
     */
    public XTFQueryHits(String [] hits) // Hits hits, IndexReader reader) 
	{
		this.hits = hits;
    }

    /**
     * {@inheritDoc}
     */
    public final int getSize() {
		return hits.length;
    }

    /**
     * {@inheritDoc}
     */
    public final ScoreNode nextScoreNode() throws IOException {
		if (++hitIndex >= hits.length) {
            return null;
        }
        String uuid = "";

		return new ScoreNode(null, 0, hits[hitIndex]);
    }

    /**
     * Skips <code>n</code> hits.
     *
     * @param n the number of hits to skip.
     * @throws IOException if an error occurs while skipping.
     */
    public void skip(int n) throws IOException {
        hitIndex += n;
    }
}
