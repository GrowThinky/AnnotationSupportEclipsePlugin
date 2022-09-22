package javafoldingstructureprovider;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ISlaveDocumentManager;
import org.eclipse.jface.text.ISlaveDocumentManagerExtension;
import org.eclipse.jface.text.projection.ProjectionDocument;
import org.eclipse.jface.text.projection.ProjectionDocumentManager;

public class MyProjectionDocumentManager extends ProjectionDocumentManager implements IDocumentListener, ISlaveDocumentManager, ISlaveDocumentManagerExtension {
	
	@Override
	protected ProjectionDocument createProjectionDocument(IDocument master) {
		return new MyProjectionDocument(master);
	}

}
