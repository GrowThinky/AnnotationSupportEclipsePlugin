package javafoldingstructureprovider;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

public class AnnotationRegion extends Region implements IRegion {
	
	public boolean inline;
	int[] annotationGrouping;

	public AnnotationRegion(int offset, int length, boolean inline, int[] annotationGrouping) {
		super(offset, length);
		this.inline = inline;
		this.annotationGrouping = annotationGrouping;
	}

}
