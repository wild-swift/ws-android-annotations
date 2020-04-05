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

import android.annotation.TargetApi;
import android.os.Build;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class ObservableListAdapter<T> implements ObservableList<T> {
    protected final List<T> wrapped;

    protected List<ListObserver> observers = new ArrayList<>();

    public ObservableListAdapter(List<T> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void addObserver(ListObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(ListObserver observer) {
        observers.remove(observer);
    }

    @Override
    public int size() {
        return wrapped.size();
    }

    @Override
    public boolean isEmpty() {
        return wrapped.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return wrapped.contains(o);
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return wrapped.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return wrapped.toArray();
    }

    @NotNull
    @Override
    public <T1> T1[] toArray(@NotNull T1[] t1s) {
        return wrapped.toArray(t1s);
    }

    @Override
    public boolean add(T t) {
        return wrapped.add(t);
    }

    @Override
    public boolean remove(Object o) {
        return wrapped.remove(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> collection) {
        return wrapped.containsAll(collection);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> collection) {
        return wrapped.addAll(collection);
    }

    @Override
    public boolean addAll(int i, @NotNull Collection<? extends T> collection) {
        return wrapped.addAll(i, collection);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> collection) {
        return wrapped.removeAll(collection);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> collection) {
        return wrapped.retainAll(collection);
    }

    @Override
    public void clear() {
        wrapped.clear();
    }

    @Override
    public T get(int i) {
        return wrapped.get(i);
    }

    @Override
    public T set(int i, T t) {
        return wrapped.set(i, t);
    }

    @Override
    public void add(int i, T t) {
        wrapped.add(i, t);
    }

    @Override
    public T remove(int i) {
        return wrapped.remove(i);
    }

    @Override
    public int indexOf(Object o) {
        return wrapped.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return wrapped.lastIndexOf(o);
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator() {
        return wrapped.listIterator();
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator(int i) {
        return wrapped.listIterator(i);
    }

    @NotNull
    @Override
    public List<T> subList(int i, int i1) {
        return buildSelf(wrapped.subList(i, i1));
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public void replaceAll(UnaryOperator<T> unaryOperator) {
        wrapped.replaceAll(unaryOperator);
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public void sort(Comparator<? super T> comparator) {
        wrapped.sort(comparator);
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public Spliterator<T> spliterator() {
        return wrapped.spliterator();
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public boolean removeIf(Predicate<? super T> predicate) {
        return wrapped.removeIf(predicate);
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public Stream<T> stream() {
        return wrapped.stream();
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public Stream<T> parallelStream() {
        return wrapped.parallelStream();
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public void forEach(Consumer<? super T> consumer) {
        wrapped.forEach(consumer);
    }

    protected List<T> buildSelf(List<T> subList) {
        return new ObservableListAdapter<>(subList);
    }

    protected void notifyItemInserted(int index) {
        List<ListObserver> observers = new ArrayList<>(this.observers);
        for (ListObserver observer : observers) {
            observer.onItemInserted(index);
        }
    }

    protected void notifyItemRemoved(int index) {
        List<ListObserver> observers = new ArrayList<>(this.observers);
        for (ListObserver observer : observers) {
            observer.onItemRemoved(index);
        }
    }

    protected void notifyItemChanged(int index) {
        List<ListObserver> observers = new ArrayList<>(this.observers);
        for (ListObserver observer : observers) {
            observer.onItemChanged(index);
        }
    }

    protected void notifyItemsReloaded() {
        List<ListObserver> observers = new ArrayList<>(this.observers);
        for (ListObserver observer : observers) {
            observer.onItemsReloaded();
        }
    }
}
