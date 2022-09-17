package org.eclipse.jface.text.source.projection;

import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ISlaveDocumentManager;
import org.eclipse.jface.text.projection.ProjectionDocumentManager;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.swt.widgets.Composite;

import javafoldingstructureprovider.MyProjectionDocumentManager;

@SuppressWarnings({ "restriction", "unused" })
public class MyJavaProjectionViewer2 extends JavaSourceViewer implements ISourceViewer{

	public MyJavaProjectionViewer2(Composite parent, IVerticalRuler ruler, IOverviewRuler overviewRuler, boolean showsAnnotationOverview, int styles, IPreferenceStore store) {
		super(parent, ruler, overviewRuler, showsAnnotationOverview, styles, store);
	}
	
	
	@Override
	protected ISlaveDocumentManager createSlaveDocumentManager() {
		return new MyProjectionDocumentManager();
	}

	


                                                            
}
 