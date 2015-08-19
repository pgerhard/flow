/*
 * Copyright 2000-2014 Vaadin Ltd.
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
package hummingbird;

import java.util.Iterator;
import java.util.List;

import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.ui.Template;

import elemental.json.JsonString;
import elemental.json.JsonValue;

public class TodoList extends Template {
    private int completeCount = 0;

    public TodoList() {
        getNode().put("remainingCount", Integer.valueOf(0));
    }

    public void addTodo(String todo) {
        StateNode todoNode = StateNode.create();
        todoNode.put("title", todo);
        getTodos().add(todoNode);
        if (todoNode.containsKey("hasTodos")) {
            todoNode.put("hasTodos", Boolean.TRUE);
        }
    }

    private List<Object> getTodos() {
        return getNode().getMultiValued("todos");
    }

    public void setCompleted(int todoIndex, boolean completed) {
        StateNode todoNode = (StateNode) getTodos().get(todoIndex);
        setCompleted(todoNode, completed);
    }

    private void setCompleted(StateNode todoNode, boolean completed) {
        if (completed && !todoNode.containsKey("completed")) {
            completeCount++;
            todoNode.put("completed", Boolean.TRUE);
        } else if (!completed && todoNode.containsKey("completed")) {
            completeCount--;
            todoNode.remove("completed");
        }
    }

    public void updateStuff() {
        StateNode node = getNode();

        updateBoolean(node, completeCount == 0, "hasNoCompleted");

        int remainingCount = getTodos().size() - completeCount;
        updateBoolean(node, remainingCount == 0, "isAllCompleted");
        updateBoolean(node, (remainingCount != 0), "hasRemaining");

        Integer remainingObj = Integer.valueOf(remainingCount);
        if (!remainingObj.equals(node.get("remainingCount"))) {
            node.put("remainingCount", remainingObj);
        }
    }

    private void updateBoolean(StateNode node, boolean value, String key) {
        if (value && !node.containsKey(key)) {
            node.put(key, Boolean.TRUE);
        } else if (!value && node.containsKey(key)) {
            node.remove(key);
        }
    }

    public boolean isCompleted(int todoIndex) {
        StateNode todoNode = (StateNode) getTodos().get(todoIndex);
        return todoNode.containsKey("completed");
    }

    @Override
    protected void onBrowserEvent(StateNode node, Element element,
            String methodName, Object[] params) {
        switch (methodName) {
        case "setAllCompleted": {
            boolean checked = ((JsonValue) params[0]).asBoolean();
            setAllCompleted(checked);
            break;
        }
        case "setTodoCompleted": {
            boolean checked = ((JsonValue) params[0]).asBoolean();
            setCompleted(element.getNode(), checked);
            break;
        }
        case "addNewTodo": {
            String text = ((JsonString) params[0]).getString();
            addTodo(text);
            element.setAttribute("value", "");
            break;
        }
        case "removeTodo": {
            StateNode todoNode = element.getNode();
            if (todoNode.containsKey("completed")) {
                completeCount--;
            }
            getNode().getMultiValued("todos").remove(todoNode);
            break;
        }
        case "clearCompleted": {
            clearCompleted();
            break;
        }
        case "labelDoubleClick": {
            updateBoolean(node, true, "editing");
            break;
        }
        case "handleEditBlur": {
            String text = ((JsonString) params[0]).getString();

            node.put("title", text);
            updateBoolean(node, false, "editing");
            break;
        }
        default:
            throw new RuntimeException(
                    "Unexpected event method name: " + methodName);
        }
        updateStuff();
    }

    public void clearCompleted() {
        List<Object> todos = getNode().getMultiValued("todos");
        Iterator<Object> iterator = todos.iterator();
        while (iterator.hasNext()) {
            StateNode todoNode = (StateNode) iterator.next();
            if (todoNode.containsKey("completed")) {
                iterator.remove();
            }
        }
        completeCount = 0;
    }

    private void setAllCompleted(boolean completed) {
        List<Object> todos = getNode().getMultiValued("todos");
        for (Object object : todos) {
            updateBoolean((StateNode) object, completed, "completed");
        }
        if (completed) {
            completeCount = todos.size();
        } else {
            completeCount = 0;
        }
    }
}