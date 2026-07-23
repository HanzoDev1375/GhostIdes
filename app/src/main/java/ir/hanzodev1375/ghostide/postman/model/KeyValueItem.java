package ir.hanzodev1375.ghostide.postman.model;

/**
 * A single editable row used by the Params, Headers and Form-body editors.
 * "enabled" mirrors Postman's checkbox behaviour: a disabled row is kept
 * around (so the user doesn't lose what they typed) but is not sent.
 */
public class KeyValueItem {

    private String key;
    private String value;
    private boolean enabled;

    public KeyValueItem() {
        this("", "", true);
    }

    public KeyValueItem(String key, String value, boolean enabled) {
        this.key = key == null ? "" : key;
        this.value = value == null ? "" : value;
        this.enabled = enabled;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isBlank() {
        return key.trim().isEmpty() && value.trim().isEmpty();
    }
}
