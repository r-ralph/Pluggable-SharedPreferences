/*
 *  Copyright 2018 Tamaki Hidetsugu (Ralph)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ms.ralph.psp;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public class PluggableSharedPreferences implements SharedPreferences {

    private final Converter keyEncoder;
    private final Converter keyDecoder;
    private final Converter valueEncoder;
    private final Converter valueDecoder;

    private final SharedPreferences base;

    private final IdentityHashMap<OnSharedPreferenceChangeListener, OnSharedPreferenceChangeListener> dummyListeners = new IdentityHashMap<>();

    private PluggableSharedPreferences(@NonNull SharedPreferences base,
                                       @NonNull Converter keyEncoder, @NonNull Converter keyDecoder,
                                       @NonNull Converter valueEncoder, @NonNull Converter valueDecoder) {
        this.base = base;
        this.keyEncoder = keyEncoder;
        this.keyDecoder = keyDecoder;
        this.valueEncoder = valueEncoder;
        this.valueDecoder = valueDecoder;
    }

    @Override
    public synchronized Map<String, ?> getAll() {
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, ?> entry : base.getAll().entrySet()) {
            String originalKey = keyDecoder.convert(entry.getKey());
            if (entry.getValue() != null) {
                String encrypted = (String) entry.getValue();
                map.put(originalKey, valueDecoder.convert(encrypted));
            } else {
                map.put(originalKey, null);
            }
        }
        return Collections.unmodifiableMap(map);
    }


    @Override
    public String getString(String key, String defValue) {
        String encodedKey = keyEncoder.convert(key);
        String encoded = base.getString(encodedKey, null);
        return encoded != null ? valueDecoder.convert(encoded) : defValue;
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        String encodedKey = keyEncoder.convert(key);
        Set<String> encoded = base.getStringSet(encodedKey, null);
        if (encoded == null) {
            return defValues;
        }
        HashSet<String> set = new HashSet<>();
        for (String s : encoded) {
            set.add(valueDecoder.convert(s));
        }
        return Collections.unmodifiableSet(set);
    }

    @Override
    public int getInt(String key, int defValue) {
        String value = getString(key, null);
        return value != null ? Integer.parseInt(value) : defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        String value = getString(key, null);
        return value != null ? Long.parseLong(value) : defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        String value = getString(key, null);
        return value != null ? Float.parseFloat(value) : defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        String value = getString(key, null);
        return value != null ? Boolean.parseBoolean(value) : defValue;
    }

    @Override
    public boolean contains(String key) {
        String originalKey = keyEncoder.convert(key);
        return base.contains(originalKey);
    }

    @Override
    public Editor edit() {
        return new PspEditor(base.edit());
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(final OnSharedPreferenceChangeListener listener) {
        OnSharedPreferenceChangeListener dummy = new OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                listener.onSharedPreferenceChanged(PluggableSharedPreferences.this, keyDecoder.convert(key));
            }
        };
        dummyListeners.put(listener, dummy);
        base.registerOnSharedPreferenceChangeListener(dummy);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        OnSharedPreferenceChangeListener wrapper = dummyListeners.get(listener);
        if (wrapper != null) {
            dummyListeners.remove(listener);
            base.unregisterOnSharedPreferenceChangeListener(wrapper);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        for (OnSharedPreferenceChangeListener w : dummyListeners.values()) {
            base.unregisterOnSharedPreferenceChangeListener(w);
        }
        super.finalize();
    }

    private class PspEditor implements Editor {

        private final Editor editor;

        private PspEditor(Editor editor) {
            this.editor = editor;
        }

        @Override
        public Editor putString(String key, @Nullable String value) {
            String encodedKey = keyEncoder.convert(key);
            editor.putString(encodedKey, value != null ? valueEncoder.convert(value) : null);
            return this;
        }

        @Override
        public Editor putStringSet(String key, @Nullable Set<String> values) {
            String encodedKey = keyEncoder.convert(key);
            if (values == null) {
                editor.putStringSet(encodedKey, null);
                return this;
            }
            HashSet<String> set = new HashSet<>();
            for (String s : values) {
                set.add(valueEncoder.convert(s));
            }
            editor.putStringSet(encodedKey, set);
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            return putString(key, String.valueOf(value));
        }

        @Override
        public Editor putLong(String key, long value) {
            return putString(key, String.valueOf(value));
        }

        @Override
        public Editor putFloat(String key, float value) {
            return putString(key, String.valueOf(value));
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            return putString(key, String.valueOf(value));
        }

        @Override
        public Editor remove(String key) {
            editor.remove(keyEncoder.convert(key));
            return this;
        }

        @Override
        public Editor clear() {
            editor.clear();
            return this;
        }

        @Override
        public boolean commit() {
            return editor.commit();
        }

        @Override
        public void apply() {
            editor.apply();
        }
    }

    public static class Builder {

        private final SharedPreferences base;

        private Converter keyEncoder;
        private Converter keyDecoder;
        private Converter valueEncoder;
        private Converter valueDecoder;

        public Builder(SharedPreferences base) {
            this.base = base;
        }

        @NonNull
        public Builder keyEncoder(@NonNull Converter converter) {
            keyEncoder = converter;
            return this;
        }

        @NonNull
        public Builder keyDecoder(@NonNull Converter converter) {
            keyDecoder = converter;
            return this;
        }

        @NonNull
        public Builder valueEncoder(@NonNull Converter converter) {
            valueEncoder = converter;
            return this;
        }

        @NonNull
        public Builder valueDecoder(@NonNull Converter converter) {
            valueDecoder = converter;
            return this;
        }

        @NonNull
        public Builder encoder(@NonNull Converter encoder) {
            keyEncoder = encoder;
            valueEncoder = encoder;
            return this;
        }

        @NonNull
        public Builder decoder(@NonNull Converter decoder) {
            keyDecoder = decoder;
            valueDecoder = decoder;
            return this;
        }

        @NonNull
        public SharedPreferences build() {
            if (keyEncoder == null) {
                throw new RuntimeException("keyEncoder isn't defined");
            }
            if (keyDecoder == null) {
                throw new RuntimeException("keyDecoder isn't defined");
            }
            if (valueEncoder == null) {
                throw new RuntimeException("valueEncoder isn't defined");
            }
            if (valueDecoder == null) {
                throw new RuntimeException("valueDecoder isn't defined");
            }
            return new PluggableSharedPreferences(base, keyEncoder, keyDecoder, valueEncoder, valueDecoder);
        }
    }
}
