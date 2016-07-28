//
// Copyright (c) 2016 Couchbase, Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
// except in compliance with the License. You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distributed under the
// License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
// either express or implied. See the License for the specific language governing permissions
// and limitations under the License.
//
package com.couchbase.lite.javascript;

import com.couchbase.lite.Emitter;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.javascript.scopes.MapGlobalScope;
import com.couchbase.lite.javascript.wrapper.CustomWrapFactory;
import com.couchbase.lite.util.Log;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrapFactory;

import java.util.Map;

class ViewMapBlockRhino implements Mapper {
    private final static String TAG = "JavaScriptEngine";

    private static WrapFactory wrapFactory = new CustomWrapFactory();
    private Scriptable globalScope;
    private MapGlobalScope mapGlobalScope;
    private Function mapFunction;

    // NOTE: Scope is sharable with multiple threads, it seems `Function` is not.
    //       Compiling javascript codes for every request makes slow.
    //       It is reason current code base re-use compiled Function.
    //       Instead of compiling for every request, use `synchronized` to protect Function
    //       https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino/Scopes_and_Contexts
    private final Object lockFunction = new Object();

    public ViewMapBlockRhino(String src) {
        org.mozilla.javascript.Context ctx = org.mozilla.javascript.Context.enter();
        try {
            ctx.setOptimizationLevel(-1);
            ctx.setWrapFactory(wrapFactory);
            mapGlobalScope = new MapGlobalScope();
            globalScope = ctx.initStandardObjects(mapGlobalScope, true);
            mapFunction = ctx.compileFunction(globalScope, src, "map", 0, null);
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }

    @Override
    public void map(Map<String, Object> document, Emitter emitter) {
        synchronized (lockFunction) {
            mapGlobalScope.setEmitter(emitter);

            org.mozilla.javascript.Context ctx = org.mozilla.javascript.Context.enter();
            try {
                ctx.setOptimizationLevel(-1);
                ctx.setWrapFactory(wrapFactory);

                Scriptable localScope = ctx.newObject(globalScope);
                localScope.setPrototype(globalScope);
                localScope.setParentScope(null);

                Object jsDocument = org.mozilla.javascript.Context.javaToJS(document, localScope);

                try {
                    mapFunction.call(ctx, localScope, null, new Object[]{jsDocument});
                } catch (org.mozilla.javascript.RhinoException e) {
                    Log.e(TAG, "Error in calling JavaScript map function", e);
                    return;
                }
            } finally {
                org.mozilla.javascript.Context.exit();
            }
        }
    }
}
