package org.eclipse.jface.text.source.projection;

import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ISlaveDocumentManager;
import org.eclipse.jface.text.projection.ProjectionDocumentManager;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.swt.widgets.Composite;

import javafoldingstructureprovider.MyProjectionDocumentManager;

@SuppressWarnings("restriction")
public class MyJavaSourceViewer extends JavaSourceViewer {

	@SuppressWarnings("restriction")
	public MyJavaSourceViewer(Composite parent, IVerticalRuler verticalRuler, IOverviewRuler overviewRuler,
			boolean showAnnotationsOverview, int styles, IPreferenceStore store) {
		super(parent, verticalRuler, overviewRuler, showAnnotationsOverview, styles, store);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected ISlaveDocumentManager createSlaveDocumentManager() {
		return new MyProjectionDocumentManager();
	}
	
	

}
