package workbenchpreferencepage;

import org.eclipse.jdt.internal.ui.text.folding.*;
import org.eclipse.jdt.ui.text.folding.IJavaFoldingPreferenceBlock;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.folding.IJavaFoldingPreferenceBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.OverlayPreferenceStore;
import org.eclipse.jdt.internal.ui.preferences.OverlayPreferenceStore.OverlayKey;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.internal.ui.*;

// org.eclipse.jdt.ui.text.folding.IJavaFoldingPreferenceBlock
public class AnnotationFoldingPreferenceBlock implements IJavaFoldingPreferenceBlock{
	
	private IPreferenceStore fStore;
	private ScopedPreferenceStore fScopedStore;
//	private OverlayPreferenceStore fOverlayStore;
//	private OverlayKey[] fKeys;
	private Map<Button, String> fCheckBoxes= new HashMap<>();
	private SelectionListener fCheckBoxListener= new SelectionListener() {
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		@Override
		public void widgetSelected(SelectionEvent e) {
			Button button= (Button) e.widget;
			fStore.setValue(fCheckBoxes.get(button), button.getSelection());
		}
	};


	@SuppressWarnings("restriction")
	public AnnotationFoldingPreferenceBlock() {
		fStore= JavaPlugin.getDefault().getPreferenceStore();
		fScopedStore = new ScopedPreferenceStore(InstanceScope.INSTANCE,
				"de.pltlab.annotationFolding");
		
		//fKeys= createKeys();
		//fOverlayStore= new OverlayPreferenceStore(fStore, fKeys);
	}

	/*
	@SuppressWarnings("restriction")
	private OverlayKey[] createKeys() {
		ArrayList<OverlayKey> overlayKeys= new ArrayList<>();

		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_FOLDING_JAVADOC));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_FOLDING_INNERTYPES));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_FOLDING_METHODS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_FOLDING_IMPORTS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_FOLDING_HEADERS));

		return overlayKeys.toArray(new OverlayKey[overlayKeys.size()]);
		
	}
	
*/
	@Override
	public Control createControl(Composite composite) {
	//	fOverlayStore.load();
	//	fOverlayStore.start();

		Composite inner= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout(1, true);
		layout.verticalSpacing= 3;
		layout.marginWidth= 0;
		inner.setLayout(layout);

		Label label= new Label(inner, SWT.LEFT);
		label.setText("Initially fold these elements:");

		addCheckBox(inner, "Comments", PreferenceConstants.EDITOR_FOLDING_JAVADOC, 0);
		addCheckBox(inner, "Header Comments", PreferenceConstants.EDITOR_FOLDING_HEADERS, 0);
		addCheckBox(inner, "Inner types", PreferenceConstants.EDITOR_FOLDING_INNERTYPES, 0);
		addCheckBox(inner, "Members", PreferenceConstants.EDITOR_FOLDING_METHODS, 0);
		addCheckBox(inner, "Imports", PreferenceConstants.EDITOR_FOLDING_IMPORTS, 0);
		
		
		Label label2= new Label(inner, SWT.LEFT);
		label2.setText("AnnotationFolding:");
		
		
		addCheckBox(inner, "Use single folding range ", "BASIC", 0);
		
		
		
		initializeFields();

		return inner;
	}

	private Button addCheckBox(Composite parent, String label, String key, int indentation) {
		Button checkBox= new Button(parent, SWT.CHECK);
		checkBox.setText(label);

		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= indentation;
		gd.horizontalSpan= 1;
		gd.grabExcessVerticalSpace= false;
		checkBox.setLayoutData(gd);
		checkBox.addSelectionListener(fCheckBoxListener);

		fCheckBoxes.put(checkBox, key);

		return checkBox;
	}

	private void initializeFields() {
		Iterator<Button> it= fCheckBoxes.keySet().iterator();
		while (it.hasNext()) {
			Button b= it.next();
			String key= fCheckBoxes.get(b);
			b.setSelection(fStore.getBoolean(key));
		}
		
	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void performOk() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void performDefaults() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

}
