package it.bstz.jsfautoreload.model;

public enum DebounceGroup {
    VIEW_STATIC,
    CLASS;

    public static DebounceGroup fromCategory(FileCategory category) {
        if (category == FileCategory.CLASS || category == FileCategory.SOURCE) {
            return CLASS;
        }
        return VIEW_STATIC;
    }
}
