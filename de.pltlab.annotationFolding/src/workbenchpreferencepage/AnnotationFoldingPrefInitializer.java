package workbenchpreferencepage;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

public class AnnotationFoldingPrefInitializer extends AbstractPreferenceInitializer {

    public AnnotationFoldingPrefInitializer() {
        System.out.println("Called");
    }

    @Override
    public void initializeDefaultPreferences() {
        ScopedPreferenceStore scopedPreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, "de.pltlab.annotationFolding");
        scopedPreferenceStore.setDefault("COMPLEX_ENABLED", "false");
        scopedPreferenceStore.setDefault("INITIAL_FOLD", "false");
    }

}