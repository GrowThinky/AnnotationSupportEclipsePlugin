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
        ScopedPreferenceStore scopedPreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, "de.uni-marburg.pages.fb12.peaches");
        scopedPreferenceStore.setDefault("BOOLEAN_VALUE", "false");
    }

}