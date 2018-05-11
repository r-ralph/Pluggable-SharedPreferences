package ms.ralph.psp;

import android.support.annotation.NonNull;

public interface ValueConverter {
    @NonNull
    String convert(@NonNull String key, @NonNull String value);
}
