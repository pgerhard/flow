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
package com.vaadin.flow.component;

import java.util.Objects;
import java.util.Optional;

import com.vaadin.flow.shared.Registration;

/**
 * A generic interface for field components and other user interface objects
 * that have a user-editable value. Emits change events whenever the value is
 * changed, either by the user or programmatically.
 *
 * @param <C>
 *            the component type
 * @param <V>
 *            the value type
 * @author Vaadin Ltd.
 */
public interface HasValue<C extends Component, V> extends HasEnabled {

    /**
     * An event fired when the value of a {@code HasValue} changes.
     *
     * @param <C>
     *            the component type
     * @param <V>
     *            the value type
     */
    class ValueChangeEvent<C extends Component, V> extends ComponentEvent<C> {

        private final V oldValue;
        private final V value;

        /**
         * Creates a new {@code ValueChange} event containing the given value,
         * originating from the given source component.
         *
         * @param component
         *            the component, not null
         * @param hasValue
         *            the HasValue instance bearing the value, not null
         * @param oldValue
         *            the previous value held by the source of this event
         * @param fromClient
         *            {@code true} if this event originates from the client,
         *            {@code false} otherwise.
         */
        public ValueChangeEvent(C component, HasValue<C, V> hasValue,
                V oldValue, boolean fromClient) {
            super(component, fromClient);
            this.oldValue = oldValue;
            value = hasValue.getValue();
        }

        /**
         * Returns the value of the source before this value change event
         * occurred.
         *
         * @return the value previously held by the source of this event
         */
        public V getOldValue() {
            return oldValue;
        }

        /**
         * Returns the new value that triggered this value change event.
         *
         * @return the new value
         */
        public V getValue() {
            return value;
        }
    }

    /**
     * A listener for value change events.
     *
     * @param <C>
     *            the component type
     * @param <V>
     *            the value type
     * @see ValueChangeEvent
     * @see Registration
     */
    @FunctionalInterface
    interface ValueChangeListener<C extends Component, V>
            extends ComponentEventListener<ValueChangeEvent<C, V>> {

        /**
         * Invoked when this listener receives a value change event from an
         * event source to which it has been added.
         *
         * @param event
         *            the received event, not null
         */
        @Override
        void onComponentEvent(ValueChangeEvent<C, V> event);
    }

    /**
     * Sets the value of this object. If the new value is not equal to
     * {@code getValue()}, fires a value change event. May throw
     * {@code IllegalArgumentException} if the value is not acceptable.
     * <p>
     * <i>Implementation note:</i> the implementing class should document
     * whether null values are accepted or not, and override
     * {@link #getEmptyValue()} if the empty value is not {@code null}.
     *
     * @param value
     *            the new value
     * @throws IllegalArgumentException
     *             if the value is invalid
     */
    void setValue(V value);

    /**
     * Returns the current value of this object.
     * <p>
     * <i>Implementation note:</i> the implementing class should document
     * whether null values may be returned or not, and override
     * {@link #getEmptyValue()} if the empty value is not {@code null}.
     *
     * @return the current value
     */
    V getValue();

    /**
     * Returns the component instance this {@code HasValue} is bound to.
     * <p>
     * The default implementation expects the {@code HasValue} to also extend
     * {@link Component}. If that is not the case, this method should be
     * overridden to return the correct {@link Component} instance.
     *
     * @return the component instance, never {@code null}
     */
    default C getComponent() {
        try {
            return (C) this;
        } catch (ClassCastException cce) {
            throw new ClassCastException(String.format(
                    "The class %1$s implementing %2$s does not extend %3$s. It should override the getComponent() method to return the component instance the %1$s is bound to. Original message: %4$s",
                    getClass().getName(), HasValue.class.getName(),
                    Component.class.getName(), cce.getMessage()));
        }
    }

    /**
     * Adds a value change listener. The listener is called when the value of
     * this {@code HasValue} is changed either by the user or programmatically.
     * <p>
     * <i>Implementation note:</i> the added listener is triggered if the value
     * property, named by {@link #getClientValuePropertyName()}, is changed so
     * that the new value does not match the previous value.
     *
     * @param listener
     *            the value change listener, not null
     * @return a registration for the listener
     */
    default Registration addValueChangeListener(
            ValueChangeListener<C, V> listener) {
        return getComponent().getElement().addPropertyChangeListener(
                getClientValuePropertyName(), event -> {
                    /*
                     * For the state tree, the default empty value of any
                     * property is {@code null}, but for any component it might
                     * be something else as defined by {@link #getEmptyValue()}.
                     * (#3496)
                     */
                    final V oldValue;
                    final V emptyValue = getEmptyValue();
                    final V propertyChangeEventValue = (V) event.getOldValue();
                    if (propertyChangeEventValue == null) {
                        oldValue = emptyValue;
                        if (Objects.equals(oldValue, getValue())) {
                            // there is no value change in component API
                            // perspective
                            return;
                        }
                    } else {
                        oldValue = propertyChangeEventValue;
                    }
                    listener.onComponentEvent(
                            new ValueChangeEvent<>(getComponent(), this,
                                    oldValue, event.isUserOriginated()));

                });
    }

    /**
     * Returns the value that represents an empty value.
     * <p>
     * By default {@link HasValue} is expected to support {@code null} as empty
     * values. Specific implementations might not support this.
     *
     * @return empty value
     */
    default V getEmptyValue() {
        return null;
    }

    /**
     * Returns the current value of this object, wrapped in an {@code Optional}.
     * <p>
     * The {@code Optional} will be empty if the value is {@code null} or
     * {@code isEmpty()} returns {@code true}.
     *
     * @return the current value, wrapped in an {@code Optional}
     */
    default Optional<V> getOptionalValue() {
        return isEmpty() ? Optional.empty() : Optional.ofNullable(getValue());
    }

    /**
     * Returns whether this {@code HasValue} is considered to be empty.
     * <p>
     * By default this is an equality check between current value and empty
     * value.
     *
     * @return {@code true} if considered empty; {@code false} if not
     */
    default boolean isEmpty() {
        return Objects.equals(getValue(), getEmptyValue());
    }

    /**
     * Resets the value to the empty one.
     * <p>
     * This is just a shorthand for resetting the value, see the methods
     * {@link #setValue(Object)} and {@link #getEmptyValue()}.
     *
     * @see #setValue(Object)
     * @see #getEmptyValue()
     */
    default void clear() {
        setValue(getEmptyValue());
    }

    /**
     * Get the client-side component's property name for the value this
     * interface is bound to.
     *
     * @return the name of the client-side property this interface is bound to
     */
    default String getClientValuePropertyName() {
        return "value";
    }

    /**
     * Get the name of the client-side change event that is fired when the value
     * property is changed.
     *
     * @return the name of the client-side change event that is fired when the
     *         value changes
     */
    default String getClientPropertyChangeEventName() {
        return getClientValuePropertyName() + "-changed";
    }

    /**
     * Sets the read-only mode of this {@code HasValue} to given mode. The user
     * can't change the value when in read-only mode.
     * <p>
     * A {@code HasValue} with a visual component in read-only mode typically
     * looks visually different to signal to the user that the value cannot be
     * edited.
     *
     * @param readOnly
     *            a boolean value specifying whether the component is put
     *            read-only mode or not
     */
    default void setReadOnly(boolean readOnly) {
        getComponent().getElement().setProperty("readonly", readOnly);
    }

    /**
     * Returns whether this {@code HasValue} is in read-only mode or not.
     *
     * @return {@code false} if the user can modify the value, {@code true} if
     *         not.
     */
    default boolean isReadOnly() {
        return getComponent().getElement().getProperty("readonly", false);
    }

    /**
     * Sets the required indicator visible or not.
     * <p>
     * If set visible, it is visually indicated in the user interface.
     *
     * @param requiredIndicatorVisible
     *            <code>true</code> to make the required indicator visible,
     *            <code>false</code> if not
     */
    default void setRequiredIndicatorVisible(boolean requiredIndicatorVisible) {
        getComponent().getElement().setProperty("required",
                requiredIndicatorVisible);
    }

    /**
     * Checks whether the required indicator is visible.
     *
     * @return <code>true</code> if visible, <code>false</code> if not
     */
    default boolean isRequiredIndicatorVisible() {
        return getComponent().getElement().getProperty("required", false);
    }
}
