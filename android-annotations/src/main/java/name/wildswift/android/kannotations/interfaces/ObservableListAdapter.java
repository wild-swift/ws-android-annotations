/*
 * Copyright (C) 2020 Wild Swift
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package name.wildswift.android.kannotations.interfaces;

import java.util.ArrayList;
import java.util.List;

public class ObservableListAdapter<T> implements ItemsDataSource<T> {
    protected final List<T> wrapped;

    protected List<ItemsObserver> observers = new ArrayList<>();

    public ObservableListAdapter(List<T> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public T get(int index) {
        return wrapped.get(index);
    }

    @Override
    public int getSize() {
        return wrapped.size();
    }

    @Override
    public void addObserver(ItemsObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(ItemsObserver observer) {
        observers.remove(observer);
    }

    protected void notifyItemInserted(int index) {
        List<ItemsObserver> observers = new ArrayList<>(this.observers);
        for (ItemsObserver observer : observers) {
            observer.onItemInserted(index);
        }
    }

    protected void notifyItemRemoved(int index) {
        List<ItemsObserver> observers = new ArrayList<>(this.observers);
        for (ItemsObserver observer : observers) {
            observer.onItemRemoved(index);
        }
    }

    protected void notifyItemChanged(int index) {
        List<ItemsObserver> observers = new ArrayList<>(this.observers);
        for (ItemsObserver observer : observers) {
            observer.onItemChanged(index);
        }
    }

    protected void notifyItemsReloaded() {
        List<ItemsObserver> observers = new ArrayList<>(this.observers);
        for (ItemsObserver observer : observers) {
            observer.onItemsReloaded();
        }
    }
}
