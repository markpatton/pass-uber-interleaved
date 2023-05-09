package org.eclipse.pass.object.model;

/**
 * Source of the Submission, from a PASS user or imported from another source
 */
public enum Source {

    /**
     * PASS source
     */
    PASS("pass"),

    /**
     * Other source
     */
    OTHER("other");

    private final String value;

    Source(String value) {
        this.value = value;
    }

    /**
     * Parse performer role
     *
     * @param s status string
     * @return parsed source
     */
    public static Source of(String s) {
        for (Source o: Source.values()) {
            if (o.value.equals(s)) {
                return o;
            }
        }

        throw new IllegalArgumentException("Invalid performer role: " + s);
    }

    /**
     * Get the string value of the Source. Can either be a 'pass' or 'other'.
     * @return The value of the Source.
     */
    public String getValue() {
        return value;
    }
}