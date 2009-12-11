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

/**
 * <code>ScoreNode</code> implements a simple container which holds a mapping
 * of full path to a score value. Note <code>path</code>, not 
 * <code>id</code>, is used to identify the node since it can be
 * easily generated from the Fedora query result.
 */
final class ScoreNode {

    /**
     * The id of a node. Not used.
     */
    private final NodeId id;

    /**
     * The score of the node.
     */
    private final float score;

	/**
	 * Path to the node.
	 */
	private final String path;

    /**
     * Creates a new <code>ScoreNode</code>.
     *
     * @param id    the node id.
     * @param score the score value.
     */
    ScoreNode(NodeId id, float score, String path) {
        this.id = id;
        this.score = score;
		this.path = path;
    }

    /**
     * @return the node id for this <code>ScoreNode</code>.
     */
    public NodeId getNodeId() {
        return id;
    }

    /**
     * @return the score for this <code>ScoreNode</code>.
     */
    public float getScore() {
        return score;
    }

    /**
     * @return the node id for this <code>ScoreNode</code>.
     */
    public String getNodePath() {
        return path;
    }
}
