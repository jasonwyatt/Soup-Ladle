package jwf.soupladle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes a field which should be bound to a view.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface Bind {
    /**
     * The android resource ID value for the binding.
     * @return
     */
    int value();
}
