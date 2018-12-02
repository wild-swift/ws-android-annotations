/*
 * Copyright (C) 2018 Wild Swift
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

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by swift
 */
@SuppressWarnings("WeakerAccess")
public class ViewDelegate<V extends View, VM> {
    protected final V view;
    protected final Context context;

    protected VM internalState;

    protected ViewDelegate(@NotNull V view) {
        this.view = view;
        context = view.getContext();
    }

    public void setupView() {
    }

    public VM validateStateForNewInput(@NotNull VM data) {
        return data;
    }

    public VM validateStateForOutput(@NotNull VM data) {
        return data;
    }

    public void onNewInternalState(@NotNull VM data) {
        internalState = data;
    }

    @Nullable
    public Bundle getState() {
        return null;
    }

    public void setState(@Nullable Bundle state) {

    }


    public void onShow() {
    }

    public void onHide() {
    }
}
