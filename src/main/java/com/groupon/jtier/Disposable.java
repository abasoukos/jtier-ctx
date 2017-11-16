package com.groupon.jtier;

/**
 * Objects of this type are returned from the {@link Ctx#onAttach(Runnable)},
 * {@link Ctx#onCancel(Runnable)} and {@link Ctx#onDetach(Runnable)} methods of {@link Ctx}.
 * <p>
 * If a callback needs to be removed before the Ctx lifetime ends, call the {@link #dispose()}
 * method.
 */
@FunctionalInterface
public interface Disposable {
    /**
     * Disposes a callback. Behavior undefined if invoked more than once.
     */
    void dispose();
}
