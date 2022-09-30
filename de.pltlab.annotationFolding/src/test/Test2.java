package test;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.projection.ProjectionDocument;
import org.eclipse.jface.text.projection.ProjectionDocumentManager;

public class Test2 {

	Document masterDocument = new Document("hello");

	ProjectionDocumentManager manager = new ProjectionDocumentManager();
	ProjectionDocument projectionDocument = (ProjectionDocument) manager.createSlaveDocument(masterDocument);
	


}
