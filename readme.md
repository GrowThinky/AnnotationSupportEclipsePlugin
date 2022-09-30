AnnotationSupportEclipsePlugin
==============================

Eclipse plug-in for folding @Annotations in Java Editor.

This is a work in progress. Come back in December 2022 for the complete plugin.

What's here:
* Basic folding of annotations (mirroring folding for methods)
* Complex preferences settings:
  * Fold annotation arguments
  * Specify certain annotations to be hidden on partial folding   

EPL License: http://www.eclipse.org/legal/epl-v20.html

Update site: https://github.com/GrowThinky/AnnotationSupportEclipsePlugin

To try out:
* Run the project as eclipse application
* Preferences->Java->Editor->Folding, select folding to use "de.pltlab.annotationFolding".
* Any newly opened Java editor will pickup annotation folding.


## Examples

´´´java
@Documented
@Constraint(validatedBy = { })
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER })
@NotNull
@Size(min = 1)

// Which may be collapsed to..
@Documented @Constraint(..) @Target(..) @NotNull @Size(..)
}
´´´
