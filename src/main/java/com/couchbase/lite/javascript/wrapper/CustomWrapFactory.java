/**
 * Copyright (c) 2015 Couchbase, Inc All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.couchbase.lite.javascript.wrapper;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrapFactory;

import java.util.List;
import java.util.Map;

public class CustomWrapFactory extends WrapFactory {

    public CustomWrapFactory() {
        setJavaPrimitiveWrap(false);
    }

    @Override
    public Scriptable wrapAsJavaObject(Context cx, Scriptable scope, Object javaObject, Class staticType) {

        if (javaObject instanceof Map) {

            NativeObject nativeObject = new NativeObject();

            nativeObject.setPrototype(ScriptableObject.getClassPrototype(scope, "Object"));

            Map<String, Object> map = (Map) javaObject;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                nativeObject.defineProperty(entry.getKey(), wrap(cx, scope, entry.getValue(), staticType), NativeObject.READONLY);
            }
            return nativeObject;
        } else if (javaObject instanceof List) {
            List list = (List) javaObject;

            Object[] newArr = new Object[list.size()];

            for (int i = 0; i < list.size(); i++) {
                newArr[i] = wrap(cx, scope, list.get(i), staticType);
            }

            NativeArray na = new NativeArray(newArr);
            na.setPrototype(ScriptableObject.getClassPrototype(scope, "Array"));
            return na;
        }

        return super.wrapAsJavaObject(cx, scope, javaObject, staticType);
    }
}
