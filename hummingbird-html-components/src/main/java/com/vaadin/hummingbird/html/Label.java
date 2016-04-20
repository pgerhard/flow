/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.html;

import com.vaadin.annotations.Tag;
import com.vaadin.ui.Component;

/**
 * Component representing a <code>&lt;label&gt;</code> element.
 *
 * @since
 * @author Vaadin Ltd
 */
@Tag(Tag.LABEL)
public class Label extends HtmlContainer {
    /**
     * Creates a new empty label.
     */
    public Label() {
        super();
    }

    /**
     * Creates a new label with the given text content.
     *
     * @param text
     *            the text content
     */
    public Label(String text) {
        this();
        setText(text);
    }

    /**
     * Sets the component that this label describes. The component (or its id)
     * should be defined in case the described component is not an ancestor of
     * the label.
     * <p>
     * The provided component must have an id set. This component will still use
     * the old id if the id of the provided component is changed after this
     * method has been called.
     *
     * @param forComponent
     *            the component that this label describes, not <code>null</code>
     *            , must have an id
     * @throws IllegalArgumentException
     *             if the provided component has no id
     */
    public void setFor(Component forComponent) {
        if (forComponent == null) {
            throw new IllegalArgumentException(
                    "The provided component cannot be null");
        }
        String forId = forComponent.getId();
        if (forId == null) {
            throw new IllegalArgumentException(
                    "The provided component must have an id");
        }
        setFor(forId);
    }

    /**
     * Sets the id of the component that this label describes. The id should be
     * defined in case the described component is not an ancestor of the label.
     *
     * @param forId
     *            the id of the described component, or <code>null</code> if
     *            there is no value
     */
    public void setFor(String forId) {
        setAttribute("for", forId);
    }

    /**
     * Gets the id of the component that this label describes.
     *
     * @see #setFor(String)
     *
     * @return the id of the described component, or <code>null</code> to remove
     *         the value
     */
    public String getFor() {
        return getAttribute("for");
    }
}
