/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.client.flow;

import com.vaadin.client.Registry;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.flow.shared.NodeFeatures;

import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * A client-side representation of a server-side state tree.
 *
 * @author Vaadin Ltd
 */
public class StateTree {
    // Double instead of Integer since GWT 2.8 doesn't box doubles
    private final JsMap<Double, StateNode> idToNode = JsCollections.map();

    private final StateNode rootNode = new StateNode(1, this);

    private final Registry registry;

    private JsMap<Integer, String> nodeFeatureDebugName;

    private boolean updateInProgress;

    /**
     * Creates a new instance connected to the given registry.
     *
     * @param registry
     *            the global registry
     */
    public StateTree(Registry registry) {
        this.registry = registry;
        registerNode(rootNode);
    }

    /**
     * Mark this tree as being updated.
     *
     * @param updateInProgress
     *            <code>true</code> if the tree is being updated,
     *            <code>false</code> if not
     * @see #isUpdateInProgress()
     */
    public void setUpdateInProgress(boolean updateInProgress) {
        assert this.updateInProgress != updateInProgress : "Inconsistent state tree updating status, expected "
                + (updateInProgress ? "no " : "") + " updates in progress.";
        this.updateInProgress = updateInProgress;
    }

    /**
     * Returns whether this tree is currently being updated by
     * {@link TreeChangeProcessor#processChanges(StateTree, JsonArray)}.
     *
     * @return <code>true</code> if being updated, <code>false</code> if not
     */
    public boolean isUpdateInProgress() {
        return updateInProgress;
    }

    /**
     * Registers a node with this tree.
     *
     * @param node
     *            the node to register
     */
    public final void registerNode(StateNode node) {
        assert node != null;
        assert node.getTree() == this;
        assert !node.isUnregistered() : "Can't re-register a node";

        Double key = getKey(node);
        assert !idToNode.has(key) : "Node " + key + " is already registered";

        idToNode.set(key, node);
    }

    private static Double getKey(StateNode node) {
        return Double.valueOf(node.getId());
    }

    /**
     * Unregisters a node from this tree. Once the node has been unregistered,
     * it can't be registered again.
     *
     * @param node
     *            the node to unregister
     */
    public void unregisterNode(StateNode node) {
        assert assertValidNode(node);
        assert node != rootNode : "Root node can't be unregistered";

        idToNode.delete(getKey(node));
        node.unregister();
    }

    /**
     * Verifies that the provided node is not null and properly registered with
     * this state tree.
     *
     * @param node
     *            the node to test
     * @return always <code>true</code>, for use with the <code>assert</code>
     *         keyword
     */
    private boolean assertValidNode(StateNode node) {
        assert node != null : "Node is null";
        assert node.getTree() == this : "Node is not created for this tree";
        assert node == getNode(
                node.getId()) : "Node id is not registered with this tree";

        return true;
    }

    /**
     * Finds the node with the given id.
     *
     * @param id
     *            the id
     * @return the node with the given id, or <code>null</code> if no such node
     *         is registered.
     */
    public StateNode getNode(int id) {
        Double key = Double.valueOf(id);

        return idToNode.get(key);
    }

    /**
     * Gets the root node of this tree.
     *
     * @return the root node
     */
    public StateNode getRootNode() {
        return rootNode;
    }

    /**
     * Sends an event to the server.
     *
     * @param node
     *            the node that listened to the event
     * @param eventType
     *            the type of event
     * @param eventData
     *            extra data associated with the event
     */
    public void sendEventToServer(StateNode node, String eventType,
            JsonObject eventData) {
        assert assertValidNode(node);
        registry.getServerConnector().sendEventMessage(node, eventType,
                eventData);
    }

    /**
     * Sends a map property sync to the server.
     *
     * @param property
     *            the property that should have its value synced to the server,
     *            not <code>null</code>
     */
    public void sendNodePropertySyncToServer(MapProperty property) {
        assert property != null;

        NodeMap nodeMap = property.getMap();
        StateNode node = nodeMap.getNode();

        assert assertValidNode(node);
        registry.getServerConnector().sendNodeSyncMessage(node, nodeMap.getId(),
                property.getName(), property.getValue());
    }

    /**
     * Sends a request to call server side method with {@code methodName} using
     * {@code argsArray} as argument values.
     *
     * @param node
     *            the node referring to the server side instance containing the
     *            method
     * @param methodName
     *            the method name
     * @param argsArray
     *            the arguments array for the method
     */
    public void sendTemplateEventToServer(StateNode node, String methodName,
            JsArray<?> argsArray) {
        assert assertValidNode(node);
        JsonArray array = WidgetUtil.crazyJsCast(argsArray);
        registry.getServerConnector().sendTemplateEventMessage(node, methodName,
                array);
    }

    /**
     * Gets the {@link Registry} that this state tree belongs to.
     *
     * @return the registry of this tree, not <code>null</code>
     */
    public Registry getRegistry() {
        return registry;
    }

    /**
     * Returns a human readable string for the name space with the given id.
     *
     * @param id
     *            the node feature id
     * @return a human readable string describing the node feature
     */
    public String getFeatureDebugName(int id) {
        if (nodeFeatureDebugName == null) {
            nodeFeatureDebugName = JsCollections.map();
            nodeFeatureDebugName.set(NodeFeatures.ELEMENT_DATA, "elementData");
            nodeFeatureDebugName.set(NodeFeatures.ELEMENT_PROPERTIES,
                    "elementProperties");
            nodeFeatureDebugName.set(NodeFeatures.ELEMENT_ATTRIBUTES,
                    "elementAttributes");
            nodeFeatureDebugName.set(NodeFeatures.ELEMENT_CHILDREN,
                    "elementChildren");
            nodeFeatureDebugName.set(NodeFeatures.ELEMENT_LISTENERS,
                    "elementListeners");
            nodeFeatureDebugName.set(NodeFeatures.UI_PUSHCONFIGURATION,
                    "pushConfiguration");
            nodeFeatureDebugName.set(
                    NodeFeatures.UI_PUSHCONFIGURATION_PARAMETERS,
                    "pushConfigurationParameters");
            nodeFeatureDebugName.set(NodeFeatures.TEXT_NODE, "textNode");
            nodeFeatureDebugName.set(NodeFeatures.POLL_CONFIGURATION,
                    "pollConfiguration");
            nodeFeatureDebugName.set(
                    NodeFeatures.RECONNECT_DIALOG_CONFIGURATION,
                    "reconnectDialogConfiguration");
            nodeFeatureDebugName.set(
                    NodeFeatures.LOADING_INDICATOR_CONFIGURATION,
                    "loadingIndicatorConfiguration");
            nodeFeatureDebugName.set(NodeFeatures.CLASS_LIST, "classList");
            nodeFeatureDebugName.set(NodeFeatures.ELEMENT_STYLE_PROPERTIES,
                    "elementStyleProperties");
            nodeFeatureDebugName.set(NodeFeatures.TEMPLATE, "template");
            nodeFeatureDebugName.set(NodeFeatures.SYNCHRONIZED_PROPERTY_EVENTS,
                    "synchronizedPropertyEvents");
            nodeFeatureDebugName.set(NodeFeatures.SYNCHRONIZED_PROPERTIES,
                    "synchronizedProperties");
            nodeFeatureDebugName.set(NodeFeatures.TEMPLATE_OVERRIDES,
                    "templateOverrides");
            nodeFeatureDebugName.set(NodeFeatures.OVERRIDE_DATA,
                    "overrideNodeData");
        }
        if (nodeFeatureDebugName.has(id)) {
            return nodeFeatureDebugName.get(id);
        } else {
            return "Unknown node feature: " + id;
        }
    }

}